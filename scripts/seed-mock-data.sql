-- Mock projects and ideas for demos and local testing. Never run this on the lab's real database.
-- Load it in one transaction, so a failure leaves nothing behind:
--   psql -U tracker -d tracker -1 -v ON_ERROR_STOP=1 -f scripts/seed-mock-data.sql
-- Running it a second time fails on the first insert (the emails are unique) and changes nothing.

-- Mock lab members. They are not allowlisted, so they can't sign in; they only fill in names and avatars.
INSERT INTO app_user (email, name) VALUES
    ('ananya.rao@example.com', 'Ananya Rao'),
    ('vikram.iyer@example.com', 'Vikram Iyer'),
    ('meera.krishnan@example.com', 'Meera Krishnan'),
    ('arjun.menon@example.com', 'Arjun Menon');

INSERT INTO project (title, description, status, owner_id, tags, created_at, updated_at)
SELECT p.title, p.description, p.status, u.id, p.tags, now() - p.started, now() - p.changed
FROM (VALUES
    ('Flux sampling benchmark on genome-scale models',
     'Compare **ACHR**, **OPTGP** and **CHRR** samplers on E. coli and yeast models.
- runtime and convergence on each model
- agreement between the samplers',
     'ACTIVE', 'da24b026@smail.iitm.ac.in', '{metabolism,flux-sampling,benchmark}'::text[], interval '120 days', interval '2 days'),
    ('Gut microbiome community models',
     'Build community metabolic models from metagenomic data and predict cross-feeding between species.',
     'ACTIVE', 'cs24b047@smail.iitm.ac.in', '{microbiome,metabolism,community-modelling}', interval '95 days', interval '1 day'),
    ('Drug targets in Mycobacterium tuberculosis',
     'Find essential reactions with flux balance analysis and rank them as possible drug targets.',
     'ACTIVE', 'ananya.rao@example.com', '{tuberculosis,drug-targets,fba}', interval '200 days', interval '6 days'),
    ('Graph neural networks for metabolic pathways',
     'Represent pathways as graphs and predict missing reactions with a GNN.',
     'PLANNING', 'vikram.iyer@example.com', '{ml,graphs,metabolism}', interval '20 days', interval '20 days'),
    ('Yeast growth on mixed sugars',
     'Model diauxic growth of S. cerevisiae on glucose and xylose and compare with plate-reader data.',
     'ACTIVE', 'meera.krishnan@example.com', '{yeast,metabolism,experiments}', interval '150 days', interval '9 days'),
    ('Reaction knockout predictions with machine learning',
     'Train a classifier on known knockouts to predict which gene deletions are lethal.',
     'ON_HOLD', 'arjun.menon@example.com', '{ml,knockouts,essentiality}', interval '240 days', interval '45 days'),
    ('Pan-genome of Klebsiella pneumoniae',
     'Assemble the pan-genome from public isolates and compare metabolic capabilities across strains.',
     'ACTIVE', 'ananya.rao@example.com', '{genomics,pan-genome,klebsiella}', interval '80 days', interval '4 days'),
    ('Enzyme-constrained models (GECKO)',
     'Add enzyme limits to the yeast model and check whether predicted growth rates improve.',
     'DONE', 'meera.krishnan@example.com', '{yeast,enzymes,metabolism}', interval '330 days', interval '60 days'),
    ('Lab Slack paper digest',
     'Weekly summary of the papers shared in Slack, posted back to the channel.',
     'PLANNING', 'cs24b047@smail.iitm.ac.in', '{tools,papers}', interval '10 days', interval '10 days'),
    ('Thermodynamic constraints for flux analysis',
     'Remove thermodynamically infeasible loops from flux solutions using estimated reaction energies.',
     'ACTIVE', 'vikram.iyer@example.com', '{thermodynamics,fba,metabolism}', interval '60 days', interval '3 days'),
    ('Antibiotic synergy from network analysis',
     'Predict pairs of antibiotics that work better together by combining knockouts in silico.',
     'ON_HOLD', 'arjun.menon@example.com', '{antibiotics,drug-targets,networks}', interval '180 days', interval '70 days'),
    ('Single-cell RNA-seq to metabolic states',
     'Map single-cell expression onto the model to see which pathways each cell type uses.',
     'PLANNING', 'da24b026@smail.iitm.ac.in', '{single-cell,transcriptomics,metabolism}', interval '15 days', interval '15 days'),
    ('Model curation guidelines',
     'Write down how the lab curates and versions its metabolic models, with a checklist for new models.',
     'DONE', 'ananya.rao@example.com', '{documentation,curation}', interval '300 days', interval '90 days'),
    ('Course notes: systems biology tutorials',
     'Jupyter tutorials on FBA, FVA and sampling for new lab members.',
     'ACTIVE', 'meera.krishnan@example.com', '{teaching,fba}', interval '50 days', interval '12 days'),
    ('Protein language models for enzyme function',
     'Use pretrained protein language models to suggest EC numbers for unannotated genes.',
     'PLANNING', 'vikram.iyer@example.com', '{ml,proteins,annotation}', interval '5 days', interval '5 days'),
    ('Shared GPU queue for the lab',
     'Set up a simple job queue so everyone can use the lab GPUs without clashing.',
     'PLANNING', 'cs24b047@smail.iitm.ac.in', '{infrastructure,gpu}', interval '3 days', interval '3 days')
) AS p(title, description, status, owner, tags, started, changed)
JOIN app_user u ON u.email = p.owner;

-- owners are members of their own projects, as when a project is created in the app
INSERT INTO project_member (project_id, user_id)
SELECT p.id, u.id
FROM (VALUES
    ('Flux sampling benchmark on genome-scale models', 'da24b026@smail.iitm.ac.in'),
    ('Flux sampling benchmark on genome-scale models', 'cs24b047@smail.iitm.ac.in'),
    ('Flux sampling benchmark on genome-scale models', 'vikram.iyer@example.com'),
    ('Gut microbiome community models', 'cs24b047@smail.iitm.ac.in'),
    ('Gut microbiome community models', 'meera.krishnan@example.com'),
    ('Drug targets in Mycobacterium tuberculosis', 'ananya.rao@example.com'),
    ('Drug targets in Mycobacterium tuberculosis', 'arjun.menon@example.com'),
    ('Drug targets in Mycobacterium tuberculosis', 'da24b026@smail.iitm.ac.in'),
    ('Graph neural networks for metabolic pathways', 'vikram.iyer@example.com'),
    ('Graph neural networks for metabolic pathways', 'cs24b047@smail.iitm.ac.in'),
    ('Yeast growth on mixed sugars', 'meera.krishnan@example.com'),
    ('Reaction knockout predictions with machine learning', 'arjun.menon@example.com'),
    ('Reaction knockout predictions with machine learning', 'vikram.iyer@example.com'),
    ('Pan-genome of Klebsiella pneumoniae', 'ananya.rao@example.com'),
    ('Enzyme-constrained models (GECKO)', 'meera.krishnan@example.com'),
    ('Enzyme-constrained models (GECKO)', 'ananya.rao@example.com'),
    ('Lab Slack paper digest', 'cs24b047@smail.iitm.ac.in'),
    ('Lab Slack paper digest', 'da24b026@smail.iitm.ac.in'),
    ('Thermodynamic constraints for flux analysis', 'vikram.iyer@example.com'),
    ('Antibiotic synergy from network analysis', 'arjun.menon@example.com'),
    ('Single-cell RNA-seq to metabolic states', 'da24b026@smail.iitm.ac.in'),
    ('Single-cell RNA-seq to metabolic states', 'meera.krishnan@example.com'),
    ('Model curation guidelines', 'ananya.rao@example.com'),
    ('Course notes: systems biology tutorials', 'meera.krishnan@example.com'),
    ('Course notes: systems biology tutorials', 'cs24b047@smail.iitm.ac.in'),
    ('Protein language models for enzyme function', 'vikram.iyer@example.com'),
    ('Shared GPU queue for the lab', 'cs24b047@smail.iitm.ac.in'),
    ('Shared GPU queue for the lab', 'da24b026@smail.iitm.ac.in')
) AS m(title, email)
JOIN project p ON p.title = m.title
JOIN app_user u ON u.email = m.email;

INSERT INTO project_link (project_id, label, url, created_at)
SELECT p.id, l.label, l.url, now() - l.added
FROM (VALUES
    ('Flux sampling benchmark on genome-scale models', 'COBRApy documentation', 'https://cobrapy.readthedocs.io', interval '110 days'),
    ('Flux sampling benchmark on genome-scale models', 'BiGG Models database', 'http://bigg.ucsd.edu', interval '100 days'),
    ('Gut microbiome community models', 'Virtual Metabolic Human (AGORA)', 'https://www.vmh.life', interval '90 days'),
    ('Enzyme-constrained models (GECKO)', 'GECKO toolbox', 'https://github.com/SysBioChalmers/GECKO', interval '320 days'),
    ('Course notes: systems biology tutorials', 'Tutorial notebooks', 'https://example.com/sysbio-tutorials', interval '40 days')
) AS l(title, label, url, added)
JOIN project p ON p.title = l.title;

INSERT INTO project_update (project_id, author_id, kind, body, created_at)
SELECT p.id, u.id, x.kind, x.body, now() - x.posted
FROM (VALUES
    ('Flux sampling benchmark on genome-scale models', 'da24b026@smail.iitm.ac.in', 'NOTE',
     'Set up the three samplers on the E. coli core model. CHRR is about **4x faster** than ACHR here.', interval '60 days'),
    ('Flux sampling benchmark on genome-scale models', 'vikram.iyer@example.com', 'NOTE',
     'Convergence checks done for iML1515:
- OPTGP needs about 5000 samples
- ACHR still drifts after 20000', interval '12 days'),
    ('Flux sampling benchmark on genome-scale models', 'da24b026@smail.iitm.ac.in', 'NOTE',
     'Started the yeast runs on the lab server. Results expected next week.', interval '2 days'),
    ('Gut microbiome community models', 'cs24b047@smail.iitm.ac.in', 'NOTE',
     'Built a 10-species community from the AGORA models. Cross-feeding of acetate shows up as expected.', interval '30 days'),
    ('Gut microbiome community models', 'meera.krishnan@example.com', 'NOTE',
     'Added the metagenomic abundances from the pilot study as weights.', interval '1 day'),
    ('Drug targets in Mycobacterium tuberculosis', 'ananya.rao@example.com', 'NOTE',
     'First pass done: 42 essential reactions in the model, 11 of them with no human homolog.', interval '20 days'),
    ('Yeast growth on mixed sugars', 'meera.krishnan@example.com', 'NOTE',
     'Plate-reader data for glucose and xylose uploaded to the shared drive.', interval '9 days'),
    ('Reaction knockout predictions with machine learning', 'arjun.menon@example.com', 'STATUS',
     'Status changed from Active to On hold', interval '45 days'),
    ('Reaction knockout predictions with machine learning', 'arjun.menon@example.com', 'NOTE',
     'Paused until the new knockout dataset is published.', interval '45 days'),
    ('Enzyme-constrained models (GECKO)', 'meera.krishnan@example.com', 'STATUS',
     'Status changed from Active to Done', interval '60 days'),
    ('Thermodynamic constraints for flux analysis', 'vikram.iyer@example.com', 'NOTE',
     'Loop removal works on the core model; the genome-scale model still has 3 loops left.', interval '3 days'),
    ('Course notes: systems biology tutorials', 'cs24b047@smail.iitm.ac.in', 'NOTE',
     'Added a notebook on flux variability analysis.', interval '12 days')
) AS x(title, email, kind, body, posted)
JOIN project p ON p.title = x.title
JOIN app_user u ON u.email = x.email;

INSERT INTO idea (title, description, author_id, project_id, tags, created_at)
SELECT i.title, i.description, u.id, p.id, i.tags, now() - i.posted
FROM (VALUES
    ('Benchmark samplers on community models', 'Do the flux samplers behave the same on multi-species models as on single organisms?',
     'cs24b047@smail.iitm.ac.in', NULL, '{flux-sampling,microbiome}'::text[], interval '25 days'),
    ('Track which models each paper uses', 'When a paper is shared in Slack, note which metabolic model it used, so we can find papers by model.',
     'da24b026@smail.iitm.ac.in', NULL, '{papers,tools}', interval '8 days'),
    ('Use LLMs to draft model annotations', 'Ask a language model to suggest gene annotations, then check them by hand.',
     'vikram.iyer@example.com', NULL, '{ml,annotation}', interval '14 days'),
    ('Weekly journal club from Slack papers', 'Pick the most discussed paper from Slack each week for journal club.',
     'meera.krishnan@example.com', NULL, '{papers,journal-club}', interval '4 days'),
    ('Shared conda environments', 'One environment file per project, so results can be reproduced on any lab machine.',
     'arjun.menon@example.com', NULL, '{infrastructure,reproducibility}', interval '35 days'),
    ('Thermodynamics in the tuberculosis model', 'Apply the loop-removal work to the M. tuberculosis drug-target list.',
     'ananya.rao@example.com', NULL, '{thermodynamics,tuberculosis}', interval '2 days'),
    ('Dashboard of model versions', 'Show which version of each lab model is current and what changed.',
     'ananya.rao@example.com', NULL, '{curation,tools}', interval '50 days'),
    ('Predict media from growth data', 'Infer the growth medium from measured growth rates using the model.',
     'meera.krishnan@example.com', NULL, '{yeast,ml}', interval '18 days'),
    ('Shared GPU queue for the lab', 'People keep clashing on the GPUs; a small queue would fix it.',
     'cs24b047@smail.iitm.ac.in', 'Shared GPU queue for the lab', '{infrastructure,gpu}', interval '6 days'),
    ('Single-cell data in metabolic models', 'Could single-cell RNA-seq tell us which pathways each cell type uses?',
     'da24b026@smail.iitm.ac.in', 'Single-cell RNA-seq to metabolic states', '{single-cell,metabolism}', interval '22 days'),
    ('Reading group on graph ML', 'A short reading group on graph neural networks before starting the pathway project.',
     'vikram.iyer@example.com', NULL, '{ml,graphs,reading-group}', interval '1 day'),
    ('Antibiotic pairs from knockout screens', 'Reuse the knockout predictions to suggest antibiotic pairs to test in the wet lab.',
     'arjun.menon@example.com', NULL, '{antibiotics,knockouts}', interval '28 days')
) AS i(title, description, author, project, tags, posted)
JOIN app_user u ON u.email = i.author
LEFT JOIN project p ON p.title = i.project;

INSERT INTO idea_vote (idea_id, user_id)
SELECT i.id, u.id
FROM (VALUES
    ('Benchmark samplers on community models', 'da24b026@smail.iitm.ac.in'),
    ('Benchmark samplers on community models', 'vikram.iyer@example.com'),
    ('Benchmark samplers on community models', 'meera.krishnan@example.com'),
    ('Weekly journal club from Slack papers', 'cs24b047@smail.iitm.ac.in'),
    ('Weekly journal club from Slack papers', 'da24b026@smail.iitm.ac.in'),
    ('Weekly journal club from Slack papers', 'ananya.rao@example.com'),
    ('Weekly journal club from Slack papers', 'arjun.menon@example.com'),
    ('Shared conda environments', 'meera.krishnan@example.com'),
    ('Shared conda environments', 'cs24b047@smail.iitm.ac.in'),
    ('Use LLMs to draft model annotations', 'arjun.menon@example.com'),
    ('Shared GPU queue for the lab', 'vikram.iyer@example.com'),
    ('Shared GPU queue for the lab', 'da24b026@smail.iitm.ac.in'),
    ('Shared GPU queue for the lab', 'ananya.rao@example.com'),
    ('Thermodynamics in the tuberculosis model', 'vikram.iyer@example.com'),
    ('Reading group on graph ML', 'cs24b047@smail.iitm.ac.in')
) AS v(title, email)
JOIN idea i ON i.title = v.title
JOIN app_user u ON u.email = v.email;

INSERT INTO idea_comment (idea_id, author_id, body, created_at)
SELECT i.id, u.id, c.body, now() - c.posted
FROM (VALUES
    ('Benchmark samplers on community models', 'vikram.iyer@example.com',
     'Community models are much bigger, so CHRR might be the only one that finishes.', interval '24 days'),
    ('Benchmark samplers on community models', 'cs24b047@smail.iitm.ac.in',
     'Good point. We could start with a 3-species model.', interval '23 days'),
    ('Weekly journal club from Slack papers', 'ananya.rao@example.com',
     'We could count the replies in the Slack thread to find the most discussed one.', interval '3 days'),
    ('Shared GPU queue for the lab', 'vikram.iyer@example.com',
     'Slurm would be the simplest option, it already runs on the department cluster.', interval '5 days'),
    ('Use LLMs to draft model annotations', 'arjun.menon@example.com',
     'Worth trying, but every suggestion must be checked against **UniProt**.', interval '13 days')
) AS c(title, email, body, posted)
JOIN idea i ON i.title = c.title
JOIN app_user u ON u.email = c.email;
