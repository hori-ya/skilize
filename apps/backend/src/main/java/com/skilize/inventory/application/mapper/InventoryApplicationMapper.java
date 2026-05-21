package com.skilize.inventory.application.mapper;

import com.skilize.inventory.application.command.*;
import com.skilize.inventory.presentation.request.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryApplicationMapper {

    public List<ItSkillDetailCommand> toCommands(List<ItSkillDetailItem> items) {
        return items.stream()
                .map(i -> new ItSkillDetailCommand(i.id(), i.itSkillId(), i.customSkillName(), i.skillLevelId(), i.remarks()))
                .toList();
    }

    public List<QualificationDetailCommand> toQualificationCommands(List<QualificationDetailItem> items) {
        return items.stream()
                .map(i -> new QualificationDetailCommand(i.id(), i.qualificationId(), i.customQualificationName(), i.acquiredYearMonth(), i.remarks()))
                .toList();
    }

    public List<SeminarDetailCommand> toSeminarCommands(List<SeminarDetailItem> items) {
        return items.stream()
                .map(i -> new SeminarDetailCommand(i.id(), i.adSeminarId(), i.seminarName(), i.seminarCategoryId(), i.attendedYearMonth(), i.remarks()))
                .toList();
    }

    public List<GoalCommand> toGoalCommands(List<GoalItem> items) {
        return items.stream()
                .map(i -> new GoalCommand(i.id(), i.goalCategory(), i.itSkillId(), i.qualificationId(), i.adSeminarId(), i.customName(), i.targetPeriod(), i.reason()))
                .toList();
    }

    public List<GoalReviewUpdateCommand> toGoalReviewUpdateCommands(List<GoalReviewUpdateItem> items) {
        return items.stream()
                .map(i -> new GoalReviewUpdateCommand(i.prevGoalId(), i.achievementStatus(), i.reviewNote()))
                .toList();
    }
}
