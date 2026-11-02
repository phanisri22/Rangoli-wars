export type Phase = 'LOBBY' | 'PLAYING' | 'FINISHED';
export type MoveType = 'PLACE' | 'SPLASH' | 'SHIELD' | 'SWAP';
export type CardType = 'SPLASH' | 'SHIELD' | 'SWAP';
export type BotLevel = 'EASY' | 'MEDIUM' | 'HARD';

export interface PlayerView {
  id: string;
  name: string;
  color: number;
  bot: boolean;
  botLevel: BotLevel | null;
  connected: boolean;
  score: number;
  cards: CardType[];
}

export interface MoveView {
  turn: number;
  round: number;
  playerId: string;
  color: number;
  type: MoveType;
  row: number;
  col: number;
  row2: number | null;
  col2: number | null;
  gained: number;
  captured: number;
}

export interface RoomView {
  code: string;
  phase: Phase;
  hostId: string;
  boardSize: number;
  maxRounds: number;
  maxPlayers: number;
  round: number;
  turnNumber: number;
  currentPlayerId: string | null;
  turnDeadline: number;
  players: PlayerView[];
  owners: number[][];
  shields: boolean[][];
  lastMove: MoveView | null;
  winnerIds: string[];
  endedEarly: boolean;
  matchId: number | null;
}

export interface RoomSummary {
  code: string;
  phase: Phase;
  hostId: string;
  players: number;
  maxPlayers: number;
  boardSize: number;
  round: number;
  maxRounds: number;
  createdAt: string;
}

export interface MoveRequest {
  type: MoveType;
  row: number;
  col: number;
  row2?: number;
  col2?: number;
}

export interface PlayerStats {
  rank: number;
  username: string;
  gamesPlayed: number;
  wins: number;
  totalTiles: number;
  rating: number;
  lastPlayed: string | null;
}

export interface PlayerResult {
  color: number;
  username: string | null;
  displayName: string;
  bot: boolean;
  botLevel: string | null;
  score: number;
  placement: number;
}

export interface MatchSummary {
  id: number;
  roomCode: string;
  boardSize: number;
  maxRounds: number;
  startedAt: string | null;
  finishedAt: string;
  endedEarly: boolean;
  totalMoves: number;
  players: PlayerResult[];
}

export interface MoveEntry {
  turn: number;
  round: number;
  color: number;
  type: MoveType;
  row: number;
  col: number;
  row2: number | null;
  col2: number | null;
  gained: number;
  captured: number;
}

export interface MatchDetail {
  match: MatchSummary;
  moves: MoveEntry[];
}

export const PLAYER_COLORS = ['#d81b60', '#f9a825', '#00897b', '#5e35b1'];
export const COLOR_NAMES = ['Rani pink', 'Haldi yellow', 'Peacock teal', 'Jamun purple'];

export const CARD_INFO: Record<CardType, { label: string; help: string }> = {
  SPLASH: { label: 'Splash', help: 'Color every unshielded tile in a 3×3 area.' },
  SHIELD: { label: 'Shield', help: 'Protect one of your tiles forever.' },
  SWAP: { label: 'Swap', help: 'Trade one of your tiles with a touching opponent tile.' },
};
