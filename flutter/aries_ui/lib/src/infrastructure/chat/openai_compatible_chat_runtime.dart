import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../application/chat/chat_runtime.dart';
import 'openai_chat_request_encoder.dart';
import 'openai_chat_response_decoder.dart';
import 'remote_chat_configuration.dart';

class OpenAiCompatibleChatRuntime implements ChatRuntime {
  OpenAiCompatibleChatRuntime({
    required this.configuration,
    http.Client? client,
    this.connectTimeout = const Duration(seconds: 30),
    this.readTimeout = const Duration(seconds: 90),
    this.encoder = const OpenAiChatRequestEncoder(),
    this.decoder = const OpenAiChatResponseDecoder(),
  }) : _client = client;

  final RemoteChatConfiguration configuration;
  final Duration connectTimeout;
  final Duration readTimeout;
  final OpenAiChatRequestEncoder encoder;
  final OpenAiChatResponseDecoder decoder;
  final http.Client? _client;

  @override
  Stream<ChatGenerationEvent> generate(ChatGenerationRequest request) async* {
    final client = _client ?? http.Client();
    try {
      final httpRequest = http.Request('POST', _endpoint())
        ..followRedirects = false
        ..headers.addAll({
          'Authorization': 'Bearer ${configuration.apiKey}',
          'Content-Type': 'application/json',
          'Accept':
              request.streamResponse ? 'text/event-stream' : 'application/json',
        })
        ..body = encoder.encode(configuration, request);
      final response = await client.send(httpRequest).timeout(connectTimeout);
      if (response.statusCode < 200 || response.statusCode >= 300) {
        final body = await response.stream.bytesToString().timeout(readTimeout);
        yield ChatGenerationFailed(
          code: 'provider.http_${response.statusCode}',
          message: _errorMessage(response.statusCode, body),
          recoverable: response.statusCode == 408 ||
              response.statusCode == 429 ||
              response.statusCode >= 500,
        );
        return;
      }

      if (request.streamResponse) {
        await for (final line in response.stream
            .transform(utf8.decoder)
            .transform(const LineSplitter())
            .timeout(readTimeout)) {
          final frame = decoder.decodeStreamLine(line);
          switch (frame) {
            case OpenAiStreamChunk():
              yield ChatGenerationChunk(frame.text);
            case OpenAiStreamDone():
              yield const ChatGenerationDone();
              return;
            case OpenAiStreamIgnored():
              break;
          }
        }
      } else {
        final body = await response.stream.bytesToString().timeout(readTimeout);
        yield ChatGenerationChunk(decoder.decodeResponse(body));
      }
      yield const ChatGenerationDone();
    } on TimeoutException {
      yield const ChatGenerationFailed(
        code: 'provider.timeout',
        message: 'The provider request timed out.',
        recoverable: true,
      );
    } on http.ClientException catch (error) {
      yield ChatGenerationFailed(
        code: 'provider.network',
        message: error.message,
        recoverable: true,
      );
    } on FormatException catch (error) {
      yield ChatGenerationFailed(
        code: 'provider.invalid_response',
        message: error.message,
        recoverable: false,
      );
    } finally {
      if (_client == null) {
        client.close();
      }
    }
  }

  Uri _endpoint() {
    final base = configuration.baseUrl.replaceFirst(RegExp(r'/+$'), '');
    return Uri.parse('$base/chat/completions');
  }

  String _errorMessage(int statusCode, String body) {
    final normalized = body.replaceAll(RegExp(r'\s+'), ' ').trim();
    final detail = normalized.length <= 320
        ? normalized
        : '${normalized.substring(0, 320)}...';
    return detail.isEmpty
        ? 'Provider returned HTTP $statusCode.'
        : 'Provider returned HTTP $statusCode: $detail';
  }
}
