package com.skilize.inventory.presentation.request;

/**
 * ITスキル明細1件のリクエスト要素。ItSkillDetailsRequest のリスト要素として使用する。
 * itSkillId と customSkillName は排他（マスタ登録スキルか自由入力スキルかのいずれか）。
 *
 * @param id              既存明細の内部 PK（全件洗い替えのためサーバー側では未使用）
 * @param itSkillId       ITスキルマスタの ID（null の場合はカスタムスキル）
 * @param customSkillName カスタムスキル名（itSkillId が null の場合のみ有効）
 * @param skillLevelId    スキルレベルマスタの ID
 * @param remarks         備考（自由記述）
 */
public record ItSkillDetailItem(Integer id, Integer itSkillId, String customSkillName,
                                int skillLevelId, String remarks) {}
