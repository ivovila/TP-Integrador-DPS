-- Las revisiones existentes no tienen una entrada histórica recuperable; NULL significa desconocida, no la actual.
alter table assistant_review add column reviewed_input text;
