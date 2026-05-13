ALTER TABLE documents
    RENAME COLUMN parsed_text TO cleaned_text;

ALTER TABLE documents
    ADD COLUMN text_length INTEGER;
