import cv2
import numpy as np

def generate_vector():
    img = cv2.imread('E:\\projects\\Lumiroom\\Lumiroom-logo-mono.png', cv2.IMREAD_UNCHANGED)
    if img is None:
        print("Failed to read image")
        return
    
    # Extract alpha channel
    if img.shape[2] == 4:
        alpha = img[:, :, 3]
    else:
        # if not RGBA, assume it's black and white and take inverse
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        _, alpha = cv2.threshold(gray, 127, 255, cv2.THRESH_BINARY_INV)

    # Find contours
    contours, hierarchy = cv2.findContours(alpha, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    
    h, w = alpha.shape
    
    path_data = ""
    for cnt in contours:
        # simplify contour
        epsilon = 0.001 * cv2.arcLength(cnt, True)
        approx = cv2.approxPolyDP(cnt, epsilon, True)
        
        if len(approx) < 3:
            continue
            
        # build path string
        for i, pt in enumerate(approx):
            x, y = pt[0]
            if i == 0:
                path_data += f"M {x},{y} "
            else:
                path_data += f"L {x},{y} "
        path_data += "Z "
        
    viewport_w = 108
    viewport_h = 108
    
    # The logo size is (w, h). We need to scale it to fit within a 108x108 viewport
    # Android adaptive icons recommend 108x108 with 72x72 safe zone
    scale = min(72.0 / w, 72.0 / h)
    
    xml = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="{w}"
    android:viewportHeight="{h}">
    <group
        android:translateX="{(w - w)/2}"
        android:translateY="{(h - h)/2}">
        <path
            android:fillColor="#FFFFFF"
            android:fillType="evenOdd"
            android:pathData="{path_data}" />
    </group>
</vector>
"""

    with open('E:\\projects\\Lumiroom\\app\\src\\main\\res\\mipmap-anydpi-v26\\ic_launcher_monochrome.xml', 'w') as f:
        f.write(xml)
        
    print("Generated ic_launcher_monochrome.xml")

if __name__ == "__main__":
    generate_vector()
