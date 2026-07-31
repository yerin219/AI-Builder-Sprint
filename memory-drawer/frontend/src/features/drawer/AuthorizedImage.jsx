import { useEffect, useState } from "react";
import { resolveApiUrl } from "../../api/http.js";
import { getAccessToken } from "../../utils/tokenStorage";

export default function AuthorizedImage({ imageUrl, alt, ...props }) {
  const [src, setSrc] = useState("");

  useEffect(() => {
    let objectUrl = "";
    const controller = new AbortController();
    const token = getAccessToken();
    let resolvedUrl;

    try {
      resolvedUrl = resolveApiUrl(imageUrl);
    } catch {
      setSrc("");
      return () => controller.abort();
    }

    fetch(resolvedUrl, { headers: token ? { Authorization: `Bearer ${token}` } : {}, signal: controller.signal })
      .then((response) => {
        if (!response.ok) throw new Error("이미지를 불러오지 못했습니다.");
        return response.blob();
      })
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob);
        setSrc(objectUrl);
      })
      .catch(() => setSrc(""));

    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [imageUrl]);

  return src ? <img src={src} alt={alt} {...props} /> : null;
}
