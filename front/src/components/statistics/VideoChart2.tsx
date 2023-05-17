import { getVideoData } from "api/statistics";
import { useState, useEffect } from "react";
import VideoPlayer from "./VideoPlayer";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ReferenceArea,
  ReferenceLine,
  ResponsiveContainer,
} from "recharts";
import styled from "styled-components";

const VideoChart = () => {
  const [angry, setAngry] = useState<Array<number>>([]);
  const [disgust, setDisgust] = useState<Array<number>>([]);
  const [scared, setScared] = useState<Array<number>>([]);
  const [happy, setHappy] = useState<Array<number>>([]);
  const [sad, setSad] = useState<Array<number>>([]);
  const [surprised, setSurprised] = useState<Array<number>>([]);
  const [neutral, setNeutral] = useState<Array<number>>([]);
  const [videoPath, setVideoPath] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [autoPlay, setAutoPlay] = useState(false);

  const result = [0, 1, 0];
  const timeData = [
    [0, 20],
    [20, 40],
    [40, 73],
  ];

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await getVideoData({
          gameid: 1,
        });

        setAngry(response.angry);
        setDisgust(response.disgust);
        setScared(response.scared);
        setHappy(response.happy);
        setSad(response.sad);
        setSurprised(response.surprised);
        setNeutral(response.neutral);
        setVideoPath(response.video_path);
        setIsLoading(false);
        console.log(videoPath);
        const angryData = angry.map((value, index) => ({ index, value }));
        console.log("asdfasdf");
        console.log(angryData);
        console.log("asdfasdf");
      } catch (err) {
        console.error(err);
      }
    };

    fetchData();
  }, []);

  // const angryData = angry.map((value, index) => ({ index, value }));
  // const disgustData = disgust.map((value, index) => ({ index, value }));
  // const scaredData = scared.map((value, index) => ({ index, value }));
  // const happyData = happy.map((value, index) => ({ index, value }));
  // const sadData = sad.map((value, index) => ({ index, value }));
  // const surprisedData = surprised.map((value, index) => ({ index, value }));
  // const neutralData = neutral.map((value, index) => ({ index, value }));

  const chartData = angry.map((value, index) => ({
    index,
    angry: value,
    disgust: disgust[index],
    scared: scared[index],
    happy: happy[index],
    sad: sad[index],
    surprised: surprised[index],
    neutral: neutral[index],
  }));

  const [isReferenceAreaHovered, setIsReferenceAreaHovered] = useState(-1);
  const [startEndTime, setStartEndTime] = useState<Array<number>>([0, 100000]);
  function handleClick(x1: number, x2: number) {
    // const activePayload = event.activePayload;
    // if (activePayload && activePayload.length > 0) {
    //   const index = activePayload[0].payload.index;
    //   console.log(event);
    // }
    setStartEndTime([x1, x2]);
    setAutoPlay(true);
  }
  const handleReferenceAreaMouseEnter = (event: any) => {
    if (event !== null) {
      setIsReferenceAreaHovered(event.activeLabel);
    } else {
      setIsReferenceAreaHovered(-1);
    }
  };
  const handleReferenceAreaMouseMove = (event: any) => {
    if (event !== null) {
      setIsReferenceAreaHovered(event.activeLabel);
    } else {
      setIsReferenceAreaHovered(-1);
    }
  };

  const handleReferenceAreaMouseLeave = () => {
    setIsReferenceAreaHovered(-1);
  };

  if (isLoading) {
    return <div>Loading...</div>;
  }

  return (
    <>
      <BoardBox>
        <ContainerBox>
          <ResponsiveContainer
            width={window.innerWidth * 0.38}
            height={window.innerHeight * 0.45}
          >
            <LineChart
              data={chartData}
              onMouseEnter={handleReferenceAreaMouseEnter}
              onMouseMove={handleReferenceAreaMouseMove}
              onMouseLeave={handleReferenceAreaMouseLeave}
            >
              {/* <CartesianGrid strokeDasharray="3 3" /> */}
              <XAxis dataKey="index" tick={false} />
              <YAxis tick={false} />
              {/* <Tooltip /> */}
              <Legend verticalAlign="bottom" align="center" />
              <Line
                type="monotone"
                dataKey="angry"
                stroke="#8884d8"
                dot={false}
                strokeWidth={3}
              />
              <Line
                type="monotone"
                dataKey="disgust"
                stroke="#888418"
                dot={false}
                strokeWidth={3}
              />
              <Line
                type="monotone"
                dataKey="scared"
                stroke="#f884d8"
                dot={false}
                strokeWidth={3}
              />
              <Line
                type="monotone"
                dataKey="happy"
                stroke="#0884d8"
                dot={false}
                strokeWidth={3}
              />
              <Line
                type="monotone"
                dataKey="sad"
                stroke="#2884d8"
                dot={false}
                strokeWidth={3}
              />
              <Line
                type="monotone"
                dataKey="surprised"
                stroke="#4834d8"
                dot={false}
                strokeWidth={3}
              />
              {timeData.map(([x1, x2], index) => (
                <ReferenceArea
                  key={`reference-${x1}-${x2}`}
                  x1={x1}
                  x2={x2}
                  stroke="black"
                  strokeOpacity={0.3}
                  fill={result[index] === 1 ? "green" : "red"}
                  fillOpacity={
                    isReferenceAreaHovered >= x1 && isReferenceAreaHovered < x2
                      ? 0.15
                      : 0.07
                  }
                  onClick={() => handleClick(x1, x2)}
                />
              ))}
              {isReferenceAreaHovered !== -1 && (
                <ReferenceLine x={isReferenceAreaHovered} stroke="red" />
              )}

              {/* <ReferenceArea
          x1={0}
          x2={20}
          stroke="black"
          strokeOpacity={0.3}
          fill="red"
          fillOpacity={
            isReferenceAreaHovered >= 0 && isReferenceAreaHovered < 20
              ? 0.15
              : 0.07
          }
          // onClick={handleClick}
        />
        <ReferenceArea
          x1={20}
          x2={40}
          stroke="black"
          strokeOpacity={0.3}
          fill="green"
          fillOpacity={
            isReferenceAreaHovered >= 20 && isReferenceAreaHovered < 40
              ? 0.15
              : 0.07
          }
          // onClick={handleClick}
        />
        <ReferenceArea
          x1={40}
          x2={73}
          stroke="black"
          strokeOpacity={0.3}
          fill="red"
          fillOpacity={
            isReferenceAreaHovered >= 40 && isReferenceAreaHovered < 73
              ? 0.15
              : 0.07
          }
          // onClick={handleClick}
        /> */}
            </LineChart>
          </ResponsiveContainer>
        </ContainerBox>
        <ContainerBox>
          <VideoPlayer
            start={startEndTime[0]}
            end={startEndTime[1]}
            autoPlay={autoPlay}
          />
        </ContainerBox>
      </BoardBox>
    </>
  );
};

const BoardBox = styled.div({
  position: "relative",
  margin: "1rem auto",
  display: "flex",
  flexDirection: "row",
  alignItems: "center",

  width: "90%",
  height: "70%",

  background: "white",
  borderRadius: 10,
  boxShadow: "5px 5px 5px rgba(0, 0, 0, 0.2)",
});

const ContainerBox = styled.div({
  position: "relative",
  margin: "2rem",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",

  width: "50%",
  height: "100%",
});

export default VideoChart;