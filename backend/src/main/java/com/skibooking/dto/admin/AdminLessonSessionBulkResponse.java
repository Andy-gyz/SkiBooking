package com.skibooking.dto.admin;

import java.util.List;

public record AdminLessonSessionBulkResponse(
        int createdCount,
        int skippedCount,
        List<AdminLessonSessionResponse> sessions) {
}
