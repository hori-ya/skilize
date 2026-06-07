import client from '../../../shared/api/client';
import type { AiChatRequest, AiChatResponse } from '../types';

export async function postAiChat(req: AiChatRequest): Promise<AiChatResponse> {
  const { data } = await client.post<AiChatResponse>('/ai/chat', req);
  return data;
}
