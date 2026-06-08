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
        return items.stream()
                .map(i -> new ItSkillDetailCommand(i.id(), i.itSkillId(), i.customSkillName(), i.skillLevelId(), i.remarks()))
                .toList();
    }

    /**
     * 資格明細リクエスト要素リストをコマンドリストに変換する。
     *
     * @param items 資格明細リクエスト要素のリスト
     * @return 資格明細コマンドのリスト
     */
    public List<QualificationDetailCommand> toQualificationCommands(List<QualificationDetailItem> items) {
        return items.stream()
                .map(i -> new QualificationDetailCommand(i.id(), i.qualificationId(), i.customQualificationName(), i.acquiredYearMonth(), i.remarks()))
                .toList();
    }

    /**
     * セミナー明細リクエスト要素リストをコマンドリストに変換する。
     *
     * @param items セミナー明細リクエスト要素のリスト
     * @return セミナー明細コマンドのリスト
     */
    public List<SeminarDetailCommand> toSeminarCommands(List<SeminarDetailItem> items) {
        return items.stream()
                .map(i -> new SeminarDetailCommand(i.id(), i.adSeminarId(), i.seminarName(), i.seminarCategoryId(), i.attendedYearMonth(), i.remarks()))
                .toList();
    }

    /**
     * 目標リクエスト要素リストをコマンドリストに変換する。
     *
     * @param items 目標リクエスト要素のリスト
     * @return 目標コマンドのリスト
     */
    public List<GoalCommand> toGoalCommands(List<GoalItem> items) {
        return items.stream()
                .map(i -> new GoalCommand(i.id(), i.goalCategory(), i.itSkillId(), i.qualificationId(), i.adSeminarId(), i.customName(), i.targetPeriod(), i.reason()))
                .toList();
    }

    /**
     * 目標振り返り更新リクエスト要素リストをコマンドリストに変換する。
     *
     * @param items 目標振り返り更新リクエスト要素のリスト
     * @return 目標振り返り更新コマンドのリスト
     */
    public List<GoalReviewUpdateCommand> toGoalReviewUpdateCommands(List<GoalReviewUpdateItem> items) {
        return items.stream()
                .map(i -> new GoalReviewUpdateCommand(i.prevGoalId(), i.achievementStatus(), i.reviewNote()))
                .toList();
    }
}
