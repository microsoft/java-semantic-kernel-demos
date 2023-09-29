package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import com.microsoft.semantickernel.chatcompletion.ChatCompletion;
import com.microsoft.semantickernel.chatcompletion.ChatHistory;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;

class SSEChatSession {
    private final SseBroadcaster broadcaster;
    private final ChatCompletion<ChatHistory> completion;
    private final ChatHistory chat;
    private final Sse sse;

    protected SSEChatSession(SseBroadcaster broadcaster, ChatCompletion<ChatHistory> completion, ChatHistory chat, Sse sse) {
        this.broadcaster = broadcaster;
        this.completion = completion;
        this.chat = chat;
        this.sse = sse;
    }

    public SseBroadcaster getBroadcaster() {
        return broadcaster;
    }

    public ChatCompletion<ChatHistory> getCompletion() {
        return completion;
    }

    public ChatHistory getChat() {
        return chat;
    }

    public Sse getSse() {
        return sse;
    }
}