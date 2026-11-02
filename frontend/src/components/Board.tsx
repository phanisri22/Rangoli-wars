import { useState } from 'react';
import { MoveType, PLAYER_COLORS, RoomView } from '../types';

interface Props {
  room: RoomView;
  mode: MoveType;
  swapFrom: { row: number; col: number } | null;
  interactive: boolean;
  onCell: (row: number, col: number) => void;
}

export default function Board({ room, mode, swapFrom, interactive, onCell }: Props) {
  const [hover, setHover] = useState<[number, number] | null>(null);
  const last = room.lastMove;

  const inSplash = (r: number, c: number) =>
    interactive && mode === 'SPLASH' && hover !== null && Math.abs(r - hover[0]) <= 1 && Math.abs(c - hover[1]) <= 1;
  const isLast = (r: number, c: number) =>
    last !== null && ((last.row === r && last.col === c) || (last.row2 === r && last.col2 === c));

  return (
    <div
      className={`board${interactive ? ' my-turn' : ''}`}
      style={{ gridTemplateColumns: `repeat(${room.boardSize}, 1fr)` }}
      onMouseLeave={() => setHover(null)}
    >
      {room.owners.flatMap((row, r) =>
        row.map((owner, c) => {
          const classes = ['cell'];
          if (owner < 0) classes.push('empty');
          if (room.shields[r][c]) classes.push('shielded');
          if (isLast(r, c)) classes.push('last');
          if (swapFrom && swapFrom.row === r && swapFrom.col === c) classes.push('selected');
          if (inSplash(r, c)) classes.push('splash');
          return (
            <button
              key={`${r}-${c}`}
              type="button"
              className={classes.join(' ')}
              style={owner >= 0 ? { background: PLAYER_COLORS[owner] } : undefined}
              onMouseEnter={() => setHover([r, c])}
              onClick={() => onCell(r, c)}
              disabled={!interactive}
              aria-label={`Row ${r + 1}, column ${c + 1}`}
            >
              {room.shields[r][c] ? '◆' : ''}
            </button>
          );
        }),
      )}
    </div>
  );
}
