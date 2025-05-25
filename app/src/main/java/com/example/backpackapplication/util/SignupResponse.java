// SignupResponse.java
package com.example.backpackapplication.util;

public class SignupResponse extends BaseResponse<SignupResponse.UserResult> {
    public static class UserResult {
        private String username;
        private String contactInfo;
        private String sessionId;

        public String getUsername() { return username; }
        public String getContactInfo() { return contactInfo; }
        public String getSessionId() { return sessionId; }
    }
}