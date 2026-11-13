package com.smartwater.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ChatDtos {
    private ChatDtos() {}

    public record ChatTurn(@NotBlank @Pattern(regexp = "user|assistant") String role,
                           @NotBlank @Size(max = 1000) String content) {}

    public record ChatReq(@NotEmpty @Size(max = 20) List<@Valid ChatTurn> messages) {}

    /** source is "ai" or "faq" (built-in answers when the AI is off or unavailable). */
    public record ChatReply(String reply, String source) {}
}
