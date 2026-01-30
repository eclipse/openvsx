-- Enable the fuzzystrmatch extension for Levenshtein distance calculations
-- This extension provides fuzzy string matching functions including levenshtein()
-- which is used by the similarity service for name squatting detection
--
-- The levenshtein function calculates the edit distance between two strings
-- This allows us to detect extensions with names that are too similar to existing ones
--
-- Reference: https://www.postgresql.org/docs/current/fuzzystrmatch.html

CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;


