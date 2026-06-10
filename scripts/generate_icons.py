import os
from PIL import Image, ImageDraw

def create_icons(source_path, res_dir):
    try:
        img = Image.open(source_path).convert("RGBA")
    except Exception as e:
        print(f"Failed to open {source_path}: {e}")
        return

    # Adaptive icon sizes (foreground usually takes up 72x72 out of 108x108 viewport, meaning the logo should be scaled to about 66% of the final size, but wait, Android adaptive icon foreground is exactly the size of the bucket, e.g. 108dp. The safe zone is 66dp. 
    # Let's create foreground images with transparent padding.
    # Sizes for adaptive icon foregrounds (108dp):
    # mdpi: 108x108
    # hdpi: 162x162
    # xhdpi: 216x216
    # xxhdpi: 324x324
    # xxxhdpi: 432x432
    
    # Legacy icon sizes (48dp):
    # mdpi: 48x48
    # hdpi: 72x72
    # xhdpi: 96x96
    # xxhdpi: 144x144
    # xxxhdpi: 192x192

    buckets = {
        "mdpi": {"legacy": 48, "adaptive": 108},
        "hdpi": {"legacy": 72, "adaptive": 162},
        "xhdpi": {"legacy": 96, "adaptive": 216},
        "xxhdpi": {"legacy": 144, "adaptive": 324},
        "xxxhdpi": {"legacy": 192, "adaptive": 432},
    }
    
    bg_color = (18, 32, 59, 255) # #12203B

    for bucket, sizes in buckets.items():
        bucket_dir = os.path.join(res_dir, f"mipmap-{bucket}")
        os.makedirs(bucket_dir, exist_ok=True)
        
        # 1. Adaptive Foreground (transparent bg)
        ad_size = sizes["adaptive"]
        # Scale logo to fit inside safe zone (approx 60% of total size)
        safe_size = int(ad_size * 0.6)
        logo_ratio = img.width / img.height
        
        if logo_ratio > 1:
            new_w = safe_size
            new_h = int(safe_size / logo_ratio)
        else:
            new_h = safe_size
            new_w = int(safe_size * logo_ratio)
            
        logo_resized = img.resize((new_w, new_h), Image.Resampling.LANCZOS)
        
        # Create foreground image
        fg_img = Image.new("RGBA", (ad_size, ad_size), (0, 0, 0, 0))
        offset_x = (ad_size - new_w) // 2
        offset_y = (ad_size - new_h) // 2
        fg_img.paste(logo_resized, (offset_x, offset_y), logo_resized)
        fg_img.save(os.path.join(bucket_dir, "ic_launcher_foreground.png"))
        
        # Monochrome (pure white where alpha > 0)
        mono_img = Image.new("RGBA", (ad_size, ad_size), (0, 0, 0, 0))
        white_logo = Image.new("RGBA", logo_resized.size, (255, 255, 255, 255))
        mono_img.paste(white_logo, (offset_x, offset_y), logo_resized)
        mono_img.save(os.path.join(bucket_dir, "ic_launcher_monochrome.png"))
        
        # 2. Legacy Icons
        leg_size = sizes["legacy"]
        # Scale logo to fit inside legacy size (approx 80% to allow padding/corners)
        leg_safe = int(leg_size * 0.8)
        if logo_ratio > 1:
            new_w = leg_safe
            new_h = int(leg_safe / logo_ratio)
        else:
            new_h = leg_safe
            new_w = int(leg_safe * logo_ratio)
            
        logo_leg = img.resize((new_w, new_h), Image.Resampling.LANCZOS)
        
        # Square legacy
        sq_img = Image.new("RGBA", (leg_size, leg_size), bg_color)
        off_x = (leg_size - new_w) // 2
        off_y = (leg_size - new_h) // 2
        sq_img.paste(logo_leg, (off_x, off_y), logo_leg)
        sq_img.save(os.path.join(bucket_dir, "ic_launcher.png"))
        
        # Round legacy
        rd_img = Image.new("RGBA", (leg_size, leg_size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(rd_img)
        draw.ellipse((0, 0, leg_size-1, leg_size-1), fill=bg_color)
        rd_img.paste(logo_leg, (off_x, off_y), logo_leg)
        rd_img.save(os.path.join(bucket_dir, "ic_launcher_round.png"))
        
    # Generate XMLs
    v26_dir = os.path.join(res_dir, "mipmap-anydpi-v26")
    os.makedirs(v26_dir, exist_ok=True)
    
    xml_content = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>
</adaptive-icon>
'''
    with open(os.path.join(v26_dir, "ic_launcher.xml"), "w") as f:
        f.write(xml_content)
    with open(os.path.join(v26_dir, "ic_launcher_round.xml"), "w") as f:
        f.write(xml_content)
        
    print("Done generating icons.")

create_icons("E:\\projects\\Lumiroom\\Lumiroom-logo-alpha.png", "E:\\projects\\Lumiroom\\app\\src\\main\\res")
