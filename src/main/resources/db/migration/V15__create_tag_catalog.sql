-- The lab's built-in tags. Tags are stored lowercased everywhere, so name is the stored form and label is how
-- the picker shows it. Any other tag people type is a custom tag and is still allowed.
CREATE TABLE tag (
    name     TEXT PRIMARY KEY CHECK (name = lower(btrim(name)) AND name <> ''),
    label    TEXT NOT NULL CHECK (btrim(label) <> ''),
    category TEXT NOT NULL CHECK (category IN ('DIRECTION', 'METHOD', 'OTHER')),
    position INT  NOT NULL
);

INSERT INTO tag (name, label, category, position) VALUES
    ('bioinformatics', 'Bioinformatics', 'DIRECTION', 1),
    ('drug discovery', 'Drug Discovery', 'DIRECTION', 2),
    ('genomics', 'Genomics', 'DIRECTION', 3),
    ('metabolic engineering', 'Metabolic Engineering', 'DIRECTION', 4),
    ('metabolic modeling', 'Metabolic Modeling', 'DIRECTION', 5),
    ('metagenomics', 'Metagenomics', 'DIRECTION', 6),
    ('synthetic biology', 'Synthetic Biology', 'DIRECTION', 7),
    ('systems biology', 'Systems Biology', 'DIRECTION', 8),
    ('theoretical biology', 'Theoretical Biology', 'DIRECTION', 9),
    ('algorithms', 'Algorithms', 'METHOD', 1),
    ('artificial intelligence', 'Artificial Intelligence', 'METHOD', 2),
    ('big data', 'Big Data', 'METHOD', 3),
    ('community modeling', 'Community Modeling', 'METHOD', 4),
    ('constraint-based modeling', 'Constraint-based Modeling', 'METHOD', 5),
    ('data analysis', 'Data Analysis', 'METHOD', 6),
    ('deep learning', 'Deep Learning', 'METHOD', 7),
    ('knowledge graphs', 'Knowledge Graphs', 'METHOD', 8),
    ('machine learning', 'Machine Learning', 'METHOD', 9),
    ('metabolic network reconstruction', 'Metabolic Network Reconstruction', 'METHOD', 10),
    ('network analysis', 'Network Analysis', 'METHOD', 11),
    ('reinforcement learning', 'Reinforcement Learning', 'METHOD', 12),
    ('statistical analysis', 'Statistical Analysis', 'METHOD', 13),
    ('systems theory & design principles', 'Systems Theory & Design Principles', 'METHOD', 14),
    ('molecular computing', 'Molecular Computing', 'OTHER', 1),
    ('open source', 'Open Source', 'OTHER', 2);
