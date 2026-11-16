#!/usr/bin/env python3
"""Build a polished course report DOCX from the project's Markdown sources."""

from __future__ import annotations

import argparse
import re
import tempfile
from pathlib import Path

from PIL import Image
from docx import Document
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt, RGBColor


BLACK = RGBColor(0, 0, 0)
DARK_BLUE = "173B6C"
MID_BLUE = "D8E5F3"
PALE_BLUE = "EEF4FA"
LIGHT_GRAY = "D9D9D9"
CODE_FILL = "F3F5F7"
BODY_FONT = "SimSun"
HEADING_FONT = "Microsoft YaHei"
CODE_FONT = "Consolas"


def set_cell_shading(cell, fill: str) -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    shading = tc_pr.find(qn("w:shd"))
    if shading is None:
        shading = OxmlElement("w:shd")
        tc_pr.append(shading)
    shading.set(qn("w:fill"), fill)


def set_cell_margins(cell, top=70, start=90, bottom=70, end=90) -> None:
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for margin, value in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{margin}"))
        if node is None:
            node = OxmlElement(f"w:{margin}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")


def set_table_borders(table) -> None:
    tbl_pr = table._tbl.tblPr
    borders = tbl_pr.first_child_found_in("w:tblBorders")
    if borders is None:
        borders = OxmlElement("w:tblBorders")
        tbl_pr.append(borders)
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        tag = borders.find(qn(f"w:{edge}"))
        if tag is None:
            tag = OxmlElement(f"w:{edge}")
            borders.append(tag)
        tag.set(qn("w:val"), "single")
        tag.set(qn("w:sz"), "4")
        tag.set(qn("w:space"), "0")
        tag.set(qn("w:color"), LIGHT_GRAY)


def set_repeat_table_header(row) -> None:
    tr_pr = row._tr.get_or_add_trPr()
    header = OxmlElement("w:tblHeader")
    header.set(qn("w:val"), "true")
    tr_pr.append(header)


def set_row_cant_split(row) -> None:
    tr_pr = row._tr.get_or_add_trPr()
    if tr_pr.find(qn("w:cantSplit")) is None:
        tr_pr.append(OxmlElement("w:cantSplit"))


def set_paragraph_shading(paragraph, fill: str) -> None:
    p_pr = paragraph._p.get_or_add_pPr()
    shading = p_pr.find(qn("w:shd"))
    if shading is None:
        shading = OxmlElement("w:shd")
        p_pr.append(shading)
    shading.set(qn("w:fill"), fill)


def set_paragraph_border(paragraph) -> None:
    p_pr = paragraph._p.get_or_add_pPr()
    borders = p_pr.find(qn("w:pBdr"))
    if borders is None:
        borders = OxmlElement("w:pBdr")
        p_pr.append(borders)
    for edge in ("top", "left", "bottom", "right"):
        border = OxmlElement(f"w:{edge}")
        border.set(qn("w:val"), "single")
        border.set(qn("w:sz"), "4")
        border.set(qn("w:color"), "DDE2E7")
        border.set(qn("w:space"), "4")
        borders.append(border)


def add_page_number(paragraph) -> None:
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = paragraph.add_run()
    fld_char_1 = OxmlElement("w:fldChar")
    fld_char_1.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = " PAGE "
    fld_char_2 = OxmlElement("w:fldChar")
    fld_char_2.set(qn("w:fldCharType"), "end")
    run._r.append(fld_char_1)
    run._r.append(instr)
    run._r.append(fld_char_2)


def set_font(run, name: str, size: float, *, bold=False, color=BLACK, italic=False) -> None:
    run.font.name = name
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    run.font.color.rgb = color


def remove_style_borders(style) -> None:
    p_pr = style.element.find(qn("w:pPr"))
    if p_pr is None:
        return
    borders = p_pr.find(qn("w:pBdr"))
    if borders is not None:
        p_pr.remove(borders)


def configure_styles(doc: Document) -> None:
    normal = doc.styles["Normal"]
    normal.font.name = BODY_FONT
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)
    normal.font.size = Pt(10.5)
    normal.font.color.rgb = BLACK
    normal.paragraph_format.line_spacing = 1.4
    normal.paragraph_format.space_after = Pt(5)

    heading_specs = {
        "Title": (24, True),
        "Heading 1": (17, True),
        "Heading 2": (14, True),
        "Heading 3": (11.5, True),
    }
    for style_name, (size, bold) in heading_specs.items():
        style = doc.styles[style_name]
        remove_style_borders(style)
        style.font.name = HEADING_FONT
        style._element.rPr.rFonts.set(qn("w:eastAsia"), HEADING_FONT)
        style.font.size = Pt(size)
        style.font.bold = bold
        style.font.color.rgb = BLACK
        style.paragraph_format.keep_with_next = True
        style.paragraph_format.space_before = Pt(10 if style_name != "Title" else 0)
        style.paragraph_format.space_after = Pt(6)

    for list_name in ("List Bullet", "List Number"):
        style = doc.styles[list_name]
        style.font.name = BODY_FONT
        style._element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)
        style.font.size = Pt(10.5)
        style.font.color.rgb = BLACK
        style.paragraph_format.line_spacing = 1.35
        style.paragraph_format.space_after = Pt(3)


def add_inline_runs(paragraph, text: str, *, size=10.5, font=BODY_FONT) -> None:
    token_re = re.compile(r"(\*\*[^*]+\*\*|`[^`]+`)")
    cursor = 0
    for match in token_re.finditer(text):
        if match.start() > cursor:
            run = paragraph.add_run(text[cursor : match.start()])
            set_font(run, font, size)
        token = match.group(0)
        if token.startswith("**"):
            run = paragraph.add_run(token[2:-2])
            set_font(run, font, size, bold=True)
        else:
            run = paragraph.add_run(token[1:-1])
            set_font(run, CODE_FONT, max(8.5, size - 1), color=RGBColor(34, 49, 63))
        cursor = match.end()
    if cursor < len(text):
        run = paragraph.add_run(text[cursor:])
        set_font(run, font, size)


def add_cover(doc: Document, title: str, metadata: list[str]) -> None:
    for _ in range(3):
        doc.add_paragraph()

    title_para = doc.add_paragraph(style="Title")
    title_para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title_para.paragraph_format.space_after = Pt(18)
    title_run = title_para.add_run(title)
    set_font(title_run, HEADING_FONT, 24, bold=True)

    subtitle = doc.add_paragraph()
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = subtitle.add_run("数据库原理与应用课程设计")
    set_font(run, HEADING_FONT, 15, bold=False)
    subtitle.paragraph_format.space_after = Pt(54)

    table = doc.add_table(rows=len(metadata), cols=2)
    table.alignment = WD_ALIGN_PARAGRAPH.CENTER
    table.autofit = False
    set_table_borders(table)
    widths = [1.35, 4.35]
    for index, line in enumerate(metadata):
        if "：" in line:
            label, value = line.split("：", 1)
        else:
            label, value = line, ""
        cells = table.rows[index].cells
        for cell_index, cell in enumerate(cells):
            cell.width = Inches(widths[cell_index])
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            set_cell_margins(cell, top=95, bottom=95, start=120, end=120)
            set_cell_shading(cell, "FFFFFF")
        label_para = cells[0].paragraphs[0]
        label_para.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        add_inline_runs(label_para, label + "：", size=11)
        value_para = cells[1].paragraphs[0]
        add_inline_runs(value_para, value, size=11)

    for _ in range(3):
        doc.add_paragraph()
    note = doc.add_paragraph()
    note.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = note.add_run("本报告中的封面信息由课程组填写")
    set_font(run, BODY_FONT, 9, italic=True)

    doc.add_page_break()


def add_contents(doc: Document, headings: list[str]) -> None:
    heading = doc.add_heading("目录", level=1)
    heading.alignment = WD_ALIGN_PARAGRAPH.CENTER
    for item in headings:
        paragraph = doc.add_paragraph()
        paragraph.paragraph_format.space_after = Pt(5)
        paragraph.paragraph_format.left_indent = Cm(0.35)
        run = paragraph.add_run(item)
        set_font(run, HEADING_FONT, 10.5)
    doc.add_page_break()


def add_image(doc: Document, source: Path, alt: str, figure_number: int) -> None:
    if not source.is_file():
        paragraph = doc.add_paragraph()
        add_inline_runs(paragraph, f"[缺失图片：{source}]")
        return

    with Image.open(source) as image:
        width_px, height_px = image.size
    aspect = height_px / max(width_px, 1)

    if aspect >= 5.5:
        with tempfile.TemporaryDirectory(prefix="animeverse-figure-") as temp_dir:
            with Image.open(source) as image:
                overlap = max(24, height_px // 100)
                panel_height = height_px // 3
                panel_paths: list[Path] = []
                for panel_index in range(3):
                    start = max(0, panel_index * panel_height - overlap)
                    end = min(
                        height_px,
                        (panel_index + 1) * panel_height + overlap,
                    )
                    if panel_index == 2:
                        end = height_px
                    panel = image.crop((0, start, width_px, end))
                    panel_path = Path(temp_dir) / f"panel-{panel_index + 1}.png"
                    panel.save(panel_path, format="PNG")
                    panel_paths.append(panel_path)

                paragraph = doc.add_paragraph()
                paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                paragraph.paragraph_format.keep_with_next = True
                paragraph.paragraph_format.space_before = Pt(5)
                paragraph.paragraph_format.space_after = Pt(4)
                for panel_index, panel_path in enumerate(panel_paths):
                    run = paragraph.add_run()
                    run.add_picture(str(panel_path), width=Inches(1.92))
                    if panel_index < len(panel_paths) - 1:
                        paragraph.add_run("  ")

        caption = doc.add_paragraph()
        caption.alignment = WD_ALIGN_PARAGRAPH.CENTER
        caption.paragraph_format.space_after = Pt(8)
        caption_run = caption.add_run(f"图 {figure_number}  {alt}")
        set_font(caption_run, BODY_FONT, 9, italic=True)
        return

    max_width = 6.3
    max_height = 6.2
    width = max_width
    if width * aspect > max_height:
        width = max_height / aspect

    paragraph = doc.add_paragraph()
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    paragraph.paragraph_format.keep_with_next = True
    paragraph.paragraph_format.space_before = Pt(5)
    paragraph.paragraph_format.space_after = Pt(4)
    run = paragraph.add_run()
    run.add_picture(str(source), width=Inches(width))

    caption = doc.add_paragraph()
    caption.alignment = WD_ALIGN_PARAGRAPH.CENTER
    caption.paragraph_format.space_after = Pt(8)
    caption_run = caption.add_run(f"图 {figure_number}  {alt}")
    set_font(caption_run, BODY_FONT, 9, italic=True)


def add_code_block(doc: Document, lines: list[str]) -> None:
    for index, line in enumerate(lines):
        paragraph = doc.add_paragraph()
        paragraph.paragraph_format.left_indent = Cm(0.35)
        paragraph.paragraph_format.right_indent = Cm(0.2)
        paragraph.paragraph_format.space_before = Pt(2 if index == 0 else 0)
        paragraph.paragraph_format.space_after = Pt(2 if index == len(lines) - 1 else 0)
        paragraph.paragraph_format.line_spacing = 1.05
        set_paragraph_shading(paragraph, CODE_FILL)
        set_paragraph_border(paragraph)
        run = paragraph.add_run(line if line else " ")
        set_font(run, CODE_FONT, 8.2, color=RGBColor(24, 24, 24))


def add_table(doc: Document, rows: list[list[str]]) -> None:
    if len(rows) < 2:
        return
    headers = rows[0]
    body = rows[1:]
    table = doc.add_table(rows=1, cols=len(headers))
    table.autofit = True
    table.style = "Table Grid"
    set_table_borders(table)

    header_cells = table.rows[0].cells
    set_row_cant_split(table.rows[0])
    for index, value in enumerate(headers):
        cell = header_cells[index]
        cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        set_cell_shading(cell, DARK_BLUE)
        set_cell_margins(cell)
        paragraph = cell.paragraphs[0]
        paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
        paragraph.paragraph_format.space_after = Pt(0)
        if len(rows) <= 9:
            paragraph.paragraph_format.keep_with_next = True
        run = paragraph.add_run(value)
        set_font(run, HEADING_FONT, 8.4, bold=True, color=RGBColor(255, 255, 255))
    set_repeat_table_header(table.rows[0])

    for row_index, values in enumerate(body):
        row = table.add_row()
        set_row_cant_split(row)
        cells = row.cells
        for col_index in range(len(headers)):
            value = values[col_index] if col_index < len(values) else ""
            cell = cells[col_index]
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            set_cell_margins(cell)
            set_cell_shading(cell, "FFFFFF" if row_index % 2 == 0 else PALE_BLUE)
            paragraph = cell.paragraphs[0]
            paragraph.paragraph_format.space_after = Pt(0)
            paragraph.paragraph_format.line_spacing = 1.08
            if len(rows) <= 9 and row_index < len(body) - 1:
                paragraph.paragraph_format.keep_with_next = True
            if col_index > 0 and len(value) <= 12:
                paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
            add_inline_runs(paragraph, value, size=8.2)

    spacer = doc.add_paragraph()
    spacer.paragraph_format.space_after = Pt(4)


def strip_markdown(value: str) -> str:
    value = value.strip()
    value = re.sub(r"`([^`]+)`", r"\1", value)
    value = re.sub(r"\*\*([^*]+)\*\*", r"\1", value)
    return value


def parse_table_row(line: str) -> list[str]:
    return [strip_markdown(cell) for cell in line.strip().strip("|").split("|")]


def is_table_separator(line: str) -> bool:
    cells = parse_table_row(line)
    return bool(cells) and all(re.fullmatch(r":?-{3,}:?", cell.replace(" ", "")) for cell in cells)


def first_pass(markdown: str) -> tuple[str, list[str], list[str]]:
    lines = markdown.splitlines()
    title = ""
    metadata: list[str] = []
    headings: list[str] = []
    in_cover = True
    for line in lines:
        if line.startswith("# ") and not title:
            title = line[2:].strip()
            continue
        if in_cover and line.startswith("## "):
            in_cover = False
        if in_cover and ("：" in line or ":" in line) and line.strip():
            metadata.append(line.strip())
        if line.startswith("## "):
            headings.append(line[3:].strip())
    return title, metadata, headings


def starts_new_page(heading: str) -> bool:
    return heading == "摘要" or bool(
        re.match(r"^第[一二三四五六七八九十百零〇两]+章(?:\s|$)", heading)
    )


def build_document(input_path: Path, output_path: Path) -> None:
    markdown = input_path.read_text(encoding="utf-8")
    title, metadata, headings = first_pass(markdown)
    if not title:
        raise ValueError("Markdown must start with an H1 title.")

    doc = Document()
    doc.core_properties.title = title
    doc.core_properties.subject = "数据库原理与应用课程设计"
    doc.core_properties.author = "AnimeVerse Course Design"
    doc.core_properties.keywords = "MySQL, Spring Boot, Vue 3, database course design"
    configure_styles(doc)

    section = doc.sections[0]
    section.page_width = Cm(21)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(1.75)
    section.bottom_margin = Cm(1.65)
    section.left_margin = Cm(1.95)
    section.right_margin = Cm(1.95)
    section.header_distance = Cm(0.8)
    section.footer_distance = Cm(0.75)
    add_page_number(section.footer.paragraphs[0])

    add_cover(doc, title, metadata)
    add_contents(doc, headings)

    lines = markdown.splitlines()
    index = 0
    seen_h1 = False
    in_cover = True
    figure_number = 0
    first_h2 = True

    while index < len(lines):
        line = lines[index]
        stripped = line.strip()

        if stripped.startswith("# ") and not seen_h1:
            seen_h1 = True
            index += 1
            continue
        if in_cover:
            if stripped.startswith("## "):
                in_cover = False
            else:
                index += 1
                continue

        if not stripped:
            index += 1
            continue

        if stripped.startswith("```"):
            code_lines: list[str] = []
            index += 1
            while index < len(lines) and not lines[index].strip().startswith("```"):
                code_lines.append(lines[index])
                index += 1
            add_code_block(doc, code_lines)
            index += 1
            continue

        if stripped.startswith("## "):
            heading = doc.add_heading(stripped[3:].strip(), level=1)
            if not first_h2 and starts_new_page(stripped[3:].strip()):
                heading.paragraph_format.page_break_before = True
            first_h2 = False
            heading.paragraph_format.space_before = Pt(0)
            index += 1
            continue

        if stripped.startswith("### "):
            heading = doc.add_heading(stripped[4:].strip(), level=2)
            heading.paragraph_format.space_before = Pt(10)
            index += 1
            continue

        if stripped.startswith("#### "):
            heading = doc.add_heading(stripped[5:].strip(), level=3)
            index += 1
            continue

        image_match = re.fullmatch(r"!\[([^\]]*)\]\(([^)]+)\)", stripped)
        if image_match:
            alt, relative = image_match.groups()
            figure_number += 1
            add_image(doc, (input_path.parent / relative).resolve(), alt, figure_number)
            index += 1
            continue

        if stripped.startswith("|") and index + 1 < len(lines) and is_table_separator(lines[index + 1]):
            table_rows = [parse_table_row(stripped)]
            index += 2
            while index < len(lines) and lines[index].strip().startswith("|"):
                table_rows.append(parse_table_row(lines[index]))
                index += 1
            add_table(doc, table_rows)
            continue

        if re.match(r"^[-*]\s+", stripped):
            paragraph = doc.add_paragraph(style="List Bullet")
            add_inline_runs(paragraph, re.sub(r"^[-*]\s+", "", stripped))
            index += 1
            continue

        ordered_item = re.match(r"^(\d+)\.\s+(.*)$", stripped)
        if ordered_item:
            number, content = ordered_item.groups()
            paragraph = doc.add_paragraph()
            paragraph.paragraph_format.left_indent = Cm(0.75)
            paragraph.paragraph_format.first_line_indent = Cm(-0.45)
            paragraph.paragraph_format.line_spacing = 1.35
            paragraph.paragraph_format.space_after = Pt(3)
            paragraph.paragraph_format.tab_stops.add_tab_stop(Cm(0.75))
            number_run = paragraph.add_run(f"{number}.")
            set_font(number_run, BODY_FONT, 10.5)
            paragraph.add_run("\t")
            add_inline_runs(paragraph, content)
            index += 1
            continue

        if stripped.startswith(">"):
            paragraph = doc.add_paragraph()
            paragraph.paragraph_format.left_indent = Cm(0.5)
            add_inline_runs(paragraph, stripped.lstrip("> "))
            index += 1
            continue

        paragraph = doc.add_paragraph()
        paragraph.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        add_inline_runs(paragraph, stripped)
        index += 1

    output_path.parent.mkdir(parents=True, exist_ok=True)
    doc.save(output_path)
    print(f"Built {output_path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    build_document(args.input.resolve(), args.output.resolve())


if __name__ == "__main__":
    main()
