from pydantic import BaseModel


class AnalyzeRequest(BaseModel):
    userId: int
    fiscalYearId: int
