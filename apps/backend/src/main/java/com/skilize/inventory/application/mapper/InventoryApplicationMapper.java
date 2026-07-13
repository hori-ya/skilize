/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * プレゼンテーション層のリクエストオブジェクトをアプリケーション層のコマンドオブジェクトへ変換するマッパー。
 * InventoryController から InventoryService へデータを渡す際に使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.application.mapper;

import com.skilize.inventory.application.command.*;
import com.skilize.inventory.presentation.request.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * プレゼンテーション層のリクエストをアプリケーション層のコマンドへ変換するマッパー。
 * InventoryController が受け取ったリクエスト要素を InventoryService に渡すコマンドへ詰め替える。
 */
@Component
public class InventoryApplicationMapper {

    /**
     * ITスキル明細リクエスト要素リストをコマンドリストに変換する。
     *
     * @param items ITスキル明細リクエスト要素のリスト
     * @return ITスキル明細コマンドのリスト
     */
    public List<ItSkillDetailCommand> toCommands(List<ItSkillDetailItem> items) {
        List<ItSkillDetailCommand> commands = new ArrayList<>();
        for (ItSkillDetailItem i : items) {
            commands.add(new ItSkillDetailCommand(i.id(), i.itSkillId(), i.customSkillName(), i.skillLevelId(), i.remarks()));
        }
        return commands;
    }

    /**
     * 資格明細リクエスト要素リストをコマンドリストに変換する。
     *
     * @param items 資格明細リクエスト要素のリスト
     * @return 資格明細コマンドのリスト
     */
    public List<QualificationDetailCommand> toQualificationCommands(List<QualificationDetailItem> items) {
        List<QualificationDetailCommand> commands = new ArrayList<>();
        for (QualificationDetailItem i : items) {
            commands.add(new QualificationDetailCommand(i.id(), i.qualificationId(), i.customQualificationName(), i.acquiredYearMonth(), i.remarks()));
        }
        return commands;
    }

    /**
     * セミナー明細リクエスト要素リストをコマンドリストに変換する。
     *
     * @param items セミナー明細リクエスト要素のリスト
     * @return セミナー明細コマンドのリスト
     */
    public List<SeminarDetailCommand> toSeminarCommands(List<SeminarDetailItem> items) {
        List<SeminarDetailCommand> commands = new ArrayList<>();
        for (SeminarDetailItem i : items) {
            commands.add(new SeminarDetailCommand(i.id(), i.adSeminarId(), i.seminarName(), i.seminarCategoryId(), i.attendedYearMonth(), i.remarks()));
        }
        return commands;
    }

    /**
     * 目標リクエスト要素リストをコマンドリストに変換する。
     *
     * @param items 目標リクエスト要素のリスト
     * @return 目標コマンドのリスト
     */
    public List<GoalCommand> toGoalCommands(List<GoalItem> items) {
        List<GoalCommand> commands = new ArrayList<>();
        for (GoalItem i : items) {
            commands.add(new GoalCommand(i.id(), i.goalCategory(), i.itSkillId(), i.qualificationId(), i.adSeminarId(), i.customName(), i.targetPeriod(), i.reason()));
        }
        return commands;
    }

    /**
     * 目標振り返り更新リクエスト要素リストをコマンドリストに変換する。
     *
     * @param items 目標振り返り更新リクエスト要素のリスト
     * @return 目標振り返り更新コマンドのリスト
     */
    public List<GoalReviewUpdateCommand> toGoalReviewUpdateCommands(List<GoalReviewUpdateItem> items) {
        List<GoalReviewUpdateCommand> commands = new ArrayList<>();
        for (GoalReviewUpdateItem i : items) {
            commands.add(new GoalReviewUpdateCommand(i.prevGoalId(), i.achievementStatus(), i.reviewNote()));
        }
        return commands;
    }
}
