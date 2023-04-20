import React, { useState } from "react";
import RoadSingleBox from "./RoadSingleBox";
import styled from "styled-components";
import { roadroadya } from "api/test";

interface Problem {
  gameType: string;
  id: number;
  answer: number[][];
  cost: number;
}

const GameBoard: React.FC = () => {
  const initialProblem: Problem = {
    gameType: "road",
    id: 1,
    answer: [
      [-1, 1, -1, 3, 2, -1, -1],
      [-1, 0, 0, 0, 0, 0, 1],
      [-1, 0, 0, 0, 0, 0, -1],
      [-1, 0, 0, 0, 0, 0, 2],
      [-1, 0, 0, 0, 0, 0, -1],
      [-1, 0, 0, 0, 0, 0, 3],
      [-1, -1, -1, -1, -1, -1, -1],
    ],
    cost: 3,
  };
  const [boardState, setBoardState] = useState(initialProblem);
  const [answerList, setAnswerList] = useState<Array<Object>>([]);
  const [clickCount, setClickCount] = useState(20);

  const getRandomNumber = (): [number, number] => {
    const numbers = [1, 2, 3, 4, 5];
    const sections = [1, 2, 3, 4];
    const numIndex = Math.floor(Math.random() * numbers.length);
    const sectionIndex = Math.floor(Math.random() * sections.length);
    const selectedNumber = numbers.splice(numIndex, 1)[0];
    const selectedSection = sections.splice(sectionIndex, 1)[0];

    switch (selectedSection) {
      case 1:
        return [selectedNumber, 0];
      case 2:
        return [0, selectedNumber];
      case 3:
        return [selectedNumber, 6];
      case 4:
        return [6, selectedNumber];
      default:
        return [0, 0];
    }
  };

  const cleanBoard = (): void => {
    const problem: Array<Array<number>> = Array(7)
      .fill(0)
      .map((_, i) =>
        Array(7)
          .fill(0)
          .map((_, j) => {
            if (i === 0 || i === 6 || j === 0 || j === 6) {
              return -1;
            }
            return 0;
          })
      );

    const destinations: number[] = [1, 2, 3, 1, 2, 3];

    while (destinations.length > 0) {
      const [x, y] = getRandomNumber();
      if (problem[y][x] === -1) {
        problem[y][x] = destinations.pop()!;
      }
    }

    const newProblem: Problem = {
      gameType: "road",
      id: boardState.id + 1,
      answer: problem,
      cost: 25,
    };
    setBoardState(newProblem);
  };

  const saveAnswer = () => {
    let newAnswerList: Array<Object> = answerList;
    newAnswerList = [
      ...answerList,
      { ...boardState, timeStamp: 222222, clicks: clickCount },
    ];
    setAnswerList(newAnswerList);
  };

  const onBoxClickHandler = (
    event: MouseEvent,
    xIndex: number,
    yIndex: number,
    rotate: number
  ) => {
    event.preventDefault();
    const itemValue = boardState.answer[yIndex][xIndex];
    if (itemValue === -1) return;
    const newBoardState = boardState.answer.map((row, rowIndex) =>
      rowIndex === yIndex
        ? row.map((value, columnIndex) =>
            columnIndex === xIndex ? (itemValue === 0 ? rotate : 0) : value
          )
        : row
    );
    setBoardState({ ...boardState, answer: newBoardState });
    setClickCount((clickCount) => clickCount - 1);
  };

  const onNextHandler = (event: React.MouseEvent<HTMLElement>): void => {
    event.preventDefault();
    saveAnswer();
    cleanBoard();
    setClickCount(20);
  };

  const onSubmitHandler = (event: React.MouseEvent<HTMLElement>): void => {
    event.preventDefault();
    // const date = new Date(); // YYMMDD
    const dummyProps = {
      method: "POST",
      url: "/assessment-centre/road",
      data: {
        userId: "ssafy",
        date: 230419,
        gameType: "road",
        propblems: answerList,
      },
    };
    console.log(dummyProps);
    roadroadya(dummyProps);
  };

  return (
    <RowFlexBox>
      <div>
        {boardState.answer.map((item, yIndex) => {
          return (
            <RowFlexBox key={yIndex}>
              {item.map((rowValue, xIndex) => {
                return (
                  <RoadSingleBox
                    key={xIndex}
                    rowValue={rowValue}
                    xIndex={xIndex}
                    yIndex={yIndex}
                    onClickHandler={onBoxClickHandler}
                  />
                );
              })}
            </RowFlexBox>
          );
        })}
      </div>
      <button style={{ height: "3rem" }} onClick={onNextHandler}>
        새 문제
      </button>
      <button style={{ height: "3rem" }} onClick={onSubmitHandler}>
        검사가 장난이야?
      </button>
      <div>{clickCount}</div>
    </RowFlexBox>
  );
};

const RowFlexBox = styled.div`
  display: flex;
  flex-direction: row;
`;

export default GameBoard;