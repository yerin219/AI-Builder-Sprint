export async function normalizeImage(file) {
    const bitmap = await createImageBitmap(file, {
        imageOrientation: "from-image",
    });

    const maxSize = 1600;
    const scale = Math.min(1, maxSize / Math.max(bitmap.width, bitmap.height));

    const width = Math.round(bitmap.width * scale);
    const height = Math.round(bitmap.height * scale);

    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;

    const context = canvas.getContext("2d");
    context.drawImage(bitmap, 0, 0, width, height);

    const blob = await new Promise((resolve) => {
        canvas.toBlob(resolve, "image/jpeg", 0.9);
    });

    return new File([blob], "memory-image.jpg", {
        type: "image/jpeg",
    });
}