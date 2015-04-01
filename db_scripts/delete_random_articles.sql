-- DELETE N articles
-- remove feedarticle_feed table before deleting, or deletion will fail
-- becaus of foreign key constraints

DELETE FROM feedarticle WHERE id IN ( SELECT id FROM feedarticle ORDER BY random() LIMIT $N ) 
