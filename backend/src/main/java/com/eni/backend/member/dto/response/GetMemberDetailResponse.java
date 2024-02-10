package com.eni.backend.member.dto.response;

import com.eni.backend.member.entity.Member;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
public class GetMemberDetailResponse {
    private String profile;
    private String nickname;
    private String email;
    private String lang;
    private List<GetMemberProblemResponse> successProblems;
    private List<GetMemberProblemResponse> failProblems;

    @Builder
    private GetMemberDetailResponse(String profile, String nickname, String email, String lang, List<GetMemberProblemResponse> successProblems, List<GetMemberProblemResponse> failProblems) {
        this.profile = profile;
        this.nickname = nickname;
        this.email = email;
        this.lang = lang;
        this.successProblems = successProblems;
        this.failProblems = failProblems;
    }

    public static GetMemberDetailResponse from(Member member, List<GetMemberProblemResponse> successProblems, List<GetMemberProblemResponse> failProblems) {
        return builder()
                .profile(member.getProfile())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .lang(member.getLang().name())
                .successProblems(successProblems)
                .failProblems(failProblems)
                .build();
    }
}
