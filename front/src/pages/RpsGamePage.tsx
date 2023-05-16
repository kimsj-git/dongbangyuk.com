import { useState, useEffect } from "react";

import Rps from "components/rps/Rps";
import { Timer } from "components/common";
import { Loading } from "components/rps";

import styled from "styled-components";
import { Box } from "@mui/material";

import { postRpsResults } from "api/rps";
import { StatusBar } from "components/game";

type Answer = {
  gameId: number;
  userId: number;
  date: string;
  gameType: string;
  rounds: {};
};

function RpsGamePage() {
  const [startTime, setStartTime] = useState<number>(new Date().getTime());
  const [settingTime, setSettingTime] = useState<number>(40);
  const [isGaming, setIsGaming] = useState<boolean>(true);
  const [round, setRound] = useState<number>(1);

  const [answer, setAnswer] = useState<Answer>({
    gameId: 1,
    userId: 1,
    date: new Date().toISOString(),
    gameType: "rps",
    rounds: {
      1: [],
      2: [],
      3: [],
    },
  });

  // 게임 스타트를 누르면 타이머 세팅
  const handleStart = () => {
    setIsGaming(false);
    if (round === 0) {
      setTimeout(() => {
        setRound(round + 1);
        setStartTime(new Date().getTime());
        setSettingTime(40);
        setIsGaming(true);
      }, 4000);
    }
  };

  // 라운드 종료
  const handleRoundEnd = () => {
    setIsGaming(false);
    setTimeout(() => {
      setRound(round + 1);
      setStartTime(new Date().getTime());
      if (round === 1) {
        setSettingTime(40);
      } else if (round === 2) {
        setSettingTime(100);
      }
      setIsGaming(true);
    }, 4000);
  };

  const handleGameEnd = () => {
    setIsGaming(false);
    // console.log('하윙', answer)
    postRpsResults(answer);
    // navigate("/");
  };

  const handleTimerExit = () => {
    if (round < 3) {
      handleRoundEnd();
      // newRound();
    } else {
      handleGameEnd();
    }
  };
  const handleAnswer = (gameHistory: object) => {
    const updatedRounds = {
      ...answer.rounds,
      [round]: gameHistory,
    };
    setAnswer({
      ...answer,
      rounds: updatedRounds,
    });
  };

  useEffect(() => {
    console.log(answer);
  }, [answer]);

  return (
    <>
      <StatusBar status='rps' gameType='rps' problemNum={round} />
      <Timer
        onExitHandler={handleTimerExit}
        startTime={startTime}
        settingTime={settingTime}
      />
      {isGaming ? (
        <>
          <GameBox>
            <Rps
              onRoundChange={handleAnswer}
              round={round}
              settingTime={settingTime}
              onGameStart={handleStart}
            />
          </GameBox>
        </>
      ) : (
        <Loading />
      )}
    </>
  );
}

// css

const GameBox = styled(Box)({
  fontSize: "2rem",
  // display: 'flex',
  justifyContent: "center",
  textAlign: "center",
  margin: "2rem",
});

export default RpsGamePage;
