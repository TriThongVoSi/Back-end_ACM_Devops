-- Document type filter support
-- Add document_type column with default GUIDE
ALTER TABLE documents ADD COLUMN IF NOT EXISTS document_type VARCHAR(50) DEFAULT 'GUIDE';

-- Add view_count for "Most viewed" sorting
ALTER TABLE documents ADD COLUMN IF NOT EXISTS view_count INTEGER DEFAULT 0;

-- Add is_pinned for "Recommended" sorting
ALTER TABLE documents ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT false;

-- Create index for type filtering
CREATE INDEX IF NOT EXISTS idx_documents_document_type ON documents(document_type);

-- Create index for sorting
CREATE INDEX IF NOT EXISTS idx_documents_view_count ON documents(view_count DESC);
CREATE INDEX IF NOT EXISTS idx_documents_is_pinned ON documents(is_pinned DESC, created_at DESC);
