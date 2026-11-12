
import { DotLottieReact } from "@lottiefiles/dotlottie-react";

export default function LottieAnimation({
  src,
  loop = true,
  autoplay = true,
  className = "",
  style = {},
}) {
  return (
    <div
      className={className}
      style={{ width: "100%", height: "100%", ...style }}
    >
      <DotLottieReact
        src={src}
        loop={loop}
        autoplay={autoplay}
        style={{ width: "100%", height: "100%" }}
      />
    </div>
  );
}
