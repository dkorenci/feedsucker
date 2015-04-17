-- DELETE N random articles
-- ugly hack, uses the fact that after setseed, same ids will be selected
--- because the random generator is reset, this may not always work
SELECT setseed(0.1);
DELETE FROM feedarticle_feed WHERE articles_id IN ( SELECT id FROM feedarticle ORDER BY random() LIMIT 1000 );
SELECT setseed(0.1);
DELETE FROM feedarticle WHERE id IN ( SELECT id FROM feedarticle ORDER BY random() LIMIT 1000 );
