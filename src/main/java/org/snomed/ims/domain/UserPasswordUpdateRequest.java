package org.snomed.ims.domain;

public record UserPasswordUpdateRequest(String currentPassword, String newPassword) {
}
