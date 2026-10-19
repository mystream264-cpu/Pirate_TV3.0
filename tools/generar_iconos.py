#!/usr/bin/env python3
import os
import shutil
from PIL import Image, ImageDraw, ImageFont

ICON_SOURCE = "recursos/ic_launcher_source.png"

MIPMAP_SIZES = {
    "app/src/main/res/mipmap-mdpi": 48,
    "app/src/main/res/mipmap-hdpi": 72,
    "app/src/main/res/mipmap-xhdpi": 96,
    "app/src/main/res/mipmap-xxhdpi": 144,
    "app/src/main/res/mipmap-xxxhdpi": 192,
}

BANNER_SPECS = [
    ("app/src/main/res/drawable-xhdpi", 320, 180),
    ("app/src/main/res/drawable-xxhdpi", 480, 270),
]

BANNER_BG_COLOR = (26, 31, 39, 255)  # #1A1F27
GOLD_TEXT_COLOR = (227, 189, 117, 255)  # #E3BD75

SERIF_FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSerif-Bold.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSerif-Regular.ttf",
    "/usr/share/fonts/truetype/freefont/FreeSerifBold.ttf",
    "/usr/share/fonts/truetype/freefont/FreeSerif.ttf",
    "DejaVuSerif-Bold.ttf",
    "DejaVuSerif.ttf",
    "Times New Roman.ttf",
    "Georgia.ttf",
]

def find_serif_font(size):
    for candidate in SERIF_FONT_CANDIDATES:
        if os.path.exists(candidate):
            try:
                return ImageFont.truetype(candidate, size)
            except Exception:
                continue
        # Also try by name lookup
        try:
            return ImageFont.truetype(candidate, size)
        except Exception:
            pass
    return None

def load_source_image():
    if os.path.exists(ICON_SOURCE):
        print(f"Usando fuente de icono: {ICON_SOURCE}")
        return Image.open(ICON_SOURCE).convert("RGBA")
    
    print(f"ADVERTENCIA: {ICON_SOURCE} no existe. Generando icono simple por defecto.")
    # Generar icono simple por defecto (1024x1024 con fondo redondeado oscuro y logo dorado)
    img = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle((32, 32, 992, 992), radius=180, fill=(26, 31, 39, 255), outline=(227, 189, 117, 255), width=16)
    draw.text((512, 512), "P", fill=(227, 189, 117, 255), anchor="mm")
    return img

def get_resample_filter():
    # Compatible across PIL / Pillow versions
    if hasattr(Image, "Resampling"):
        return Image.Resampling.LANCZOS
    return Image.LANCZOS

def apply_circle_mask(image):
    width, height = image.size
    mask = Image.new("L", (width, height), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, width, height), fill=255)
    
    output = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    output.paste(image, (0, 0), mask=mask)
    return output

def generate_launcher_icons(source_img):
    resample = get_resample_filter()
    
    # 1. Eliminar mipmap-anydpi-v26 si existe
    anydpi_path = "app/src/main/res/mipmap-anydpi-v26"
    if os.path.exists(anydpi_path):
        print(f"Eliminando {anydpi_path}...")
        shutil.rmtree(anydpi_path)

    # 2. Generar mipmaps
    for out_dir, size in MIPMAP_SIZES.items():
        os.makedirs(out_dir, exist_ok=True)
        
        # Eliminar posibles webp obsoletos
        for old_file in ["ic_launcher.webp", "ic_launcher_round.webp"]:
            p = os.path.join(out_dir, old_file)
            if os.path.exists(p):
                os.remove(p)

        # ic_launcher.png (manteniendo proporción con LANCZOS)
        square_img = source_img.resize((size, size), resample=resample)
        launcher_path = os.path.join(out_dir, "ic_launcher.png")
        square_img.save(launcher_path, format="PNG")
        print(f"Generado: {launcher_path} ({size}x{size})")

        # ic_launcher_round.png (máscara circular sobre la misma imagen)
        round_img = apply_circle_mask(square_img)
        round_path = os.path.join(out_dir, "ic_launcher_round.png")
        round_img.save(round_path, format="PNG")
        print(f"Generado: {round_path} ({size}x{size})")

def generate_tv_banners(source_img):
    resample = get_resample_filter()
    
    for out_dir, w, h in BANNER_SPECS:
        os.makedirs(out_dir, exist_ok=True)
        banner = Image.new("RGBA", (w, h), BANNER_BG_COLOR)
        
        # Icono con altura ~75% del banner, manteniendo proporción
        icon_h = int(round(h * 0.75))
        src_w, src_h = source_img.size
        icon_w = int(round(icon_h * (src_w / src_h)))
        icon_resized = source_img.resize((icon_w, icon_h), resample=resample)
        
        # Buscar fuente serif para texto "Pirate_TV" en dorado #E3BD75
        font_size = int(round(h * 0.28))
        font = find_serif_font(font_size)
        text = "Pirate_TV"
        
        if font is not None:
            # Medir texto
            draw_temp = ImageDraw.Draw(banner)
            bbox = draw_temp.textbbox((0, 0), text, font=font)
            text_w = bbox[2] - bbox[0]
            text_h = bbox[3] - bbox[1]
            
            spacing = int(round(w * 0.04))
            total_content_w = icon_w + spacing + text_w
            
            # Si el contenido excede el ancho, reducir fuente
            if total_content_w > int(w * 0.92):
                font_size = int(round(font_size * (int(w * 0.92) - icon_w - spacing) / max(1, text_w)))
                font = find_serif_font(font_size)
                if font is not None:
                    bbox = draw_temp.textbbox((0, 0), text, font=font)
                    text_w = bbox[2] - bbox[0]
                    text_h = bbox[3] - bbox[1]
                    total_content_w = icon_w + spacing + text_w

            start_x = max(10, (w - total_content_w) // 2)
            icon_y = (h - icon_h) // 2
            
            # Pegar icono
            banner.paste(icon_resized, (start_x, icon_y), mask=icon_resized)
            
            # Dibujar texto
            text_x = start_x + icon_w + spacing
            text_y = (h - text_h) // 2 - bbox[1]
            draw = ImageDraw.Draw(banner)
            draw.text((text_x, text_y), text, fill=GOLD_TEXT_COLOR, font=font)
            print(f"Banner TV con texto serif creado: {w}x{h} en {out_dir}")
        else:
            print(f"Fuente serif no encontrada para banner {w}x{h}, omitiendo texto.")
            # Centrar el icono
            icon_x = (w - icon_w) // 2
            icon_y = (h - icon_h) // 2
            banner.paste(icon_resized, (icon_x, icon_y), mask=icon_resized)
            
        banner_path = os.path.join(out_dir, "banner.png")
        banner.save(banner_path, format="PNG")
        print(f"Generado: {banner_path} ({w}x{h})")

def main():
    print("=== Generador de Iconos y Banners Pirate_TV ===")
    source = load_source_image()
    generate_launcher_icons(source)
    generate_tv_banners(source)
    print("=== Proceso de generación completado exitosamente ===")

if __name__ == "__main__":
    main()
