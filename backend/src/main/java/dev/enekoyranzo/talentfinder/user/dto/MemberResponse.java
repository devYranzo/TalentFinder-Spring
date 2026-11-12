package dev.enekoyranzo.talentfinder.user.dto;

import dev.enekoyranzo.talentfinder.user.User;

public record MemberResponse(Long id, String firstName, String lastName) {

    public static MemberResponse from(User u) {
        return new MemberResponse(u.getId(), u.getFirstName(), u.getLastName());
    }
}