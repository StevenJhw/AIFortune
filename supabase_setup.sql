-- Run this in Supabase SQL Editor

-- User data table (profile + fortune results)
CREATE TABLE user_data (
    android_id TEXT PRIMARY KEY,
    profile JSONB,
    results JSONB,
    overall_summary TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Interactive history (question/answer records)
CREATE TABLE interactive_history (
    id BIGSERIAL PRIMARY KEY,
    android_id TEXT NOT NULL,
    method TEXT NOT NULL,
    question TEXT NOT NULL,
    result TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_interactive_history_android_id ON interactive_history(android_id);

-- Enable Row Level Security
ALTER TABLE user_data ENABLE ROW LEVEL SECURITY;
ALTER TABLE interactive_history ENABLE ROW LEVEL SECURITY;

-- Allow all operations with anon key (since we use android_id, not auth)
CREATE POLICY "Allow all for user_data" ON user_data FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all for interactive_history" ON interactive_history FOR ALL USING (true) WITH CHECK (true);
