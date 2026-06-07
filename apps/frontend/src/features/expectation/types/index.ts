/** TL/会社からの期待コメント。AI 分析の方向性づけに使用される（分析結果には直接引用しない）。 */
export interface UserExpectation {
  tlExpectation: string | null;
  companyExpectation: string | null;
}
