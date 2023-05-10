import { useState } from "react";

import { GameBoard } from "../components/turnFigure";
import { StatusBar, GameTemplate } from "components/game";

function TurnPicGamePage() {
  const [status] = useState("explain");
  const [problemNum, setProblemNum] = useState<number>(1);

  return (
    <GameTemplate>
      <StatusBar status={status} gameType='road' problemNum={problemNum} />
      <GameBoard
        problemNum={problemNum}
        ascProblemNum={() => setProblemNum(problemNum + 1)}
      />
      {/* <Timer startTime={startTime} settingTime={3} /> */}
    </GameTemplate>
  );
}

export default TurnPicGamePage;
