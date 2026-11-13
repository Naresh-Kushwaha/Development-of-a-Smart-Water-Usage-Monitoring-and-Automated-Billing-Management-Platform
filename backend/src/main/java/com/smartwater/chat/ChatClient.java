package com.smartwater.chat;

import java.util.List;

/** The language-model side of the assistant. An interface so ChatService can be unit tested with a fake. */
public interface ChatClient {
    record Turn(String role, String content) {}

    /** true when ANTHROPIC_API_KEY is set. */
    boolean configured();

    /** Sends the system prompt and the conversation; returns the assistant's reply text. */
    String complete(String system, List<Turn> messages);
}
