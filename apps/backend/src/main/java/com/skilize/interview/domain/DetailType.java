package com.skilize.interview.domain;

/**
 * 面談ノートの対象明細種別。InterviewDetailNote がどの明細に紐づくかを識別するために使用する。
 * IT_SKILL      = ITスキル明細（it_skill_details）
 * QUALIFICATION = 資格明細（qualification_details）
 * SEMINAR       = セミナー明細（seminar_details）
 * GOAL          = 目標明細（inventory_goals）
 */
public enum DetailType {
    IT_SKILL, QUALIFICATION, SEMINAR, GOAL
}
