package com.smartwater.web;

import com.smartwater.security.Current;
import com.smartwater.service.ChatService;
import com.smartwater.web.ChatDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService service;

    /** The browser sends the whole conversation each time (the server keeps no chat history). */
    @PostMapping
    public ChatReply chat(@Valid @RequestBody ChatReq req) {
        return service.chat(Current.user(), req.messages());
    }
}
