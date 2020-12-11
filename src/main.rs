use image::Rgb;
use image::RgbImage;
use rusttype::{point, Font, PositionedGlyph, Scale};

fn get_args() -> (String, String) {
    let ver = env!("CARGO_PKG_VERSION");
    let args = clap::App::new("text-to-image")
        .arg(
            clap::Arg::with_name("text")
                .long("text")
                .short("t")
                .default_value("你好！")
                .help("Text to draw"),
        )
        .arg(
            clap::Arg::with_name("output")
                .long("output")
                .short("o")
                .help("Output path")
                .default_value("image.png"),
        )
        .version(ver)
        .get_matches();
    let text = args.value_of("text").unwrap();
    let output = args.value_of("output").unwrap();
    (text.to_owned(), output.to_owned())
}

fn to_glyphs<'a>(font: &'a Font, text: &str) -> Vec<PositionedGlyph<'a>> {
    let scale = Scale::uniform(64.0);
    let v_metrics = font.v_metrics(scale);
    font.layout(&text, scale, point(5.0, 5.0 + v_metrics.ascent))
        .collect()
}

fn main() {
    let (text, output) = get_args();
    let font_data =
        include_bytes!("../TextToImage/app/src/main/assets/fonts/YingZhangKaiShu-2.ttf");
    let font = Font::from_bytes(font_data as &[u8]).expect("Error constructing Font");

    let lines: Vec<_> = text
        .chars()
        .collect::<Vec<char>>()
        .chunks(6)
        .map(|c| c.iter().collect::<String>())
        .map(|s| to_glyphs(&font, &s))
        .collect();

    // work out the layout size
    let glyphs_height = {
        let min_y = lines
            .iter()
            .flat_map(|g| {
                g.iter()
                    .filter(|g| g.pixel_bounding_box().is_some())
                    .map(|g| g.pixel_bounding_box().unwrap().min.y)
            })
            .min()
            .unwrap();
        let max_y = lines
            .iter()
            .flat_map(|g| {
                g.iter()
                    .filter(|g| g.pixel_bounding_box().is_some())
                    .map(|g| g.pixel_bounding_box().unwrap().max.y)
            })
            .max()
            .unwrap();
        (max_y - min_y) as u32
    };
    let glyphs_width = {
        let min_x = lines
            .iter()
            .flat_map(|g| {
                g.iter()
                    .filter(|g| g.pixel_bounding_box().is_some())
                    .map(|g| g.pixel_bounding_box().unwrap().min.x)
            })
            .min()
            .unwrap();
        let max_x = lines
            .iter()
            .flat_map(|g| {
                g.iter()
                    .filter(|g| g.pixel_bounding_box().is_some())
                    .map(|g| g.pixel_bounding_box().unwrap().max.x)
            })
            .max()
            .unwrap();
        (max_x - min_x) as u32
    };

    let glyphs_height = glyphs_height + 10;

    let w = glyphs_width + 20;
    let h = glyphs_height * lines.len() as u32 + 20;
    let mut image = RgbImage::from_raw(w, h, vec![255; 3 * w as usize * h as usize]).unwrap();
    for (i, glyphs) in lines.iter().enumerate() {
        for glyph in glyphs {
            if let Some(bounding_box) = glyph.pixel_bounding_box() {
                glyph.draw(|x, y, v| {
                    let offset = glyphs_height * i as u32;
                    let px = if v < 0.02 {
                        Rgb([255, 255, 255])
                    } else {
                        Rgb([(255.0 * v) as u8, 0, 0])
                    };
                    image.put_pixel(
                        x + bounding_box.min.x.max(0) as u32,
                        y + bounding_box.min.y.max(0) as u32 + offset,
                        px,
                    )
                });
            }
        }
    }
    image.save(&output).unwrap();
    println!("Generated: {}", output);
}
