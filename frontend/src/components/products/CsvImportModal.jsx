import { useState, useRef } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, Box, Typography, LinearProgress, Chip, Stack
} from '@mui/material';
import { CloudUpload, InsertDriveFile, Download } from '@mui/icons-material';
import { importCsv, downloadErrors } from '../../api/apiClient';

export default function CsvImportModal({ open, onClose, onImportComplete }) {
  const [step, setStep] = useState(1);
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const fileInputRef = useRef(null);

  const resetState = () => {
    setStep(1);
    setFile(null);
    setResult(null);
    setError('');
  };

  const handleClose = () => {
    if (step === 2) return;
    resetState();
    onClose();
  };

  const handleFileSelect = (selectedFile) => {
    if (selectedFile && selectedFile.name.endsWith('.csv')) {
      setFile(selectedFile);
      setError('');
    } else {
      setError('Please select a .csv file');
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    const droppedFile = e.dataTransfer.files[0];
    handleFileSelect(droppedFile);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  const handleUpload = async () => {
    if (!file) return;
    setStep(2);
    setError('');
    try {
      const response = await importCsv(file);
      setResult(response.data);
      setStep(3);
    } catch (err) {
      setError(err.response?.data?.message || 'Import failed. Please try again.');
      setStep(1);
    }
  };

  const handleDownloadErrors = async () => {
    if (!result?.errorFileId) return;
    try {
      const response = await downloadErrors(result.errorFileId);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'import-errors.csv');
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch {
      setError('Failed to download error report.');
    }
  };

  const handleDone = () => {
    resetState();
    onImportComplete();
  };

  const formatSize = (bytes) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Import Products from CSV</DialogTitle>
      <DialogContent>
        {step === 1 && (
          <Box>
            <Box
              onDrop={handleDrop}
              onDragOver={handleDragOver}
              onClick={() => fileInputRef.current?.click()}
              sx={{
                border: '2px dashed',
                borderColor: 'grey.400',
                borderRadius: 2,
                p: 4,
                textAlign: 'center',
                cursor: 'pointer',
                '&:hover': { borderColor: 'primary.main', bgcolor: 'action.hover' }
              }}
            >
              <CloudUpload sx={{ fontSize: 48, color: 'grey.500', mb: 1 }} />
              <Typography>Drag & drop a CSV file here</Typography>
              <Typography variant="body2" color="text.secondary">
                or click to browse
              </Typography>
            </Box>
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv"
              hidden
              onChange={(e) => handleFileSelect(e.target.files[0])}
            />
            {file && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 2 }}>
                <InsertDriveFile color="primary" />
                <Typography>{file.name}</Typography>
                <Typography variant="body2" color="text.secondary">
                  ({formatSize(file.size)})
                </Typography>
              </Box>
            )}
            {error && (
              <Typography color="error" sx={{ mt: 1 }}>{error}</Typography>
            )}
          </Box>
        )}

        {step === 2 && (
          <Box sx={{ py: 3 }}>
            <Typography sx={{ mb: 2 }}>Uploading and processing...</Typography>
            <LinearProgress />
          </Box>
        )}

        {step === 3 && result && (
          <Box sx={{ py: 1 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Import Results</Typography>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
              <Chip label={`Total: ${result.totalRows}`} variant="outlined" />
              <Chip label={`Successful: ${result.successCount}`} color="success" />
              {result.errorCount > 0 && (
                <Chip label={`Failed: ${result.errorCount}`} color="error" />
              )}
            </Stack>
            {result.errorFileId && result.errorCount > 0 && (
              <Button
                startIcon={<Download />}
                variant="outlined"
                color="error"
                onClick={handleDownloadErrors}
              >
                Download Error Report
              </Button>
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        {step === 1 && (
          <>
            <Button onClick={handleClose}>Cancel</Button>
            <Button onClick={handleUpload} variant="contained" disabled={!file}>
              Upload
            </Button>
          </>
        )}
        {step === 3 && (
          <Button onClick={handleDone} variant="contained">Close</Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
