import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

function Home() {
  const [items, setItems] = useState<string[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [uploadResult, setUploadResult] = useState<string | null>(null)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)

  const fetchList = async () => {
    setLoading(true)
    setError(null)
    try {
      const resp = await fetch('http://localhost:8080/api')
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
      const data: string[] = await resp.json()
      setItems(data)
    } catch (e: any) {
      setError(e?.message || 'Failed to fetch')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchList()
  }, [])

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setUploadResult(null)
    const file = e.target.files && e.target.files[0]
    if (!file) {
      setSelectedFile(null)
      return
    }
    setSelectedFile(file)
  }

  const handleUploadConfirm = async () => {
    if (!selectedFile) return
    setUploadResult(null)
    try {
      const text = await selectedFile.text()
      let json: any
      try {
        json = JSON.parse(text)
      } catch (parseErr) {
        setUploadResult('Invalid JSON file')
        return
      }

      const resp = await fetch('http://localhost:8080/api', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(json),
      })

      const respText = await resp.text()
      if (!resp.ok) {
        setUploadResult(`Upload failed: ${resp.status} ${respText}`)
        return
      }
      setUploadResult(`Success: ${respText}`)
      setSelectedFile(null)
      // clear the file input element value (if needed)
      const input = document.getElementById('fileUpload') as HTMLInputElement | null
      if (input) input.value = ''
      // refresh list after successful post
      fetchList()
    } catch (err: any) {
      setUploadResult(err?.message || 'Upload error')
    }
  }

  if (loading) return <div className="p-6">Loading...</div>
  if (error) return <div className="p-6 text-red-600">Error: {error}</div>

  return (
    <div className="min-h-screen bg-gray-50 p-6 flex flex-col items-center">
      <div className="w-full max-w-3xl">
        <h1 className="text-3xl font-bold mb-4 text-gray-900">Schedule jobs</h1>

        <section className="mb-6 w-full bg-white p-4 rounded shadow">
            <label htmlFor="fileUpload" className="block text-sm font-medium text-gray-700 mb-2">Upload JSON schedule:</label>
            <input
              id="fileUpload"
              type="file"
              accept="application/json,.json"
              onChange={handleFileChange}
              className="block w-full text-sm text-gray-900 border border-gray-300 rounded p-2"
            />
            <div className="mt-3 flex items-center gap-2">
              <button
                type="button"
                onClick={handleUploadConfirm}
                disabled={!selectedFile}
                className="px-3 py-1 bg-blue-600 text-white rounded disabled:opacity-50"
              >
                Upload
              </button>
            </div>
            {uploadResult && <div className="mt-2 text-sm text-gray-700">{uploadResult}</div>}
        </section>

        <ul className="w-full bg-white rounded shadow divide-y divide-gray-100">
          {items.map((item) => {
            const path = `/jobs/${encodeURIComponent(item)}`
            const schedulePath = `/jobs/${encodeURIComponent(item)}/schedule`
            return (
                <>
                    <li key={item} className="p-3">
                        <Link to={path} className="text-blue-600 hover:underline">{item}</Link>
                    </li>

                    <li key={item + '-schedule'} className="p-3">
                        <Link to={schedulePath} className="text-green-600 hover:underline">Schedule for {item}</Link>
                    </li>
                </>
            )
          })}
        </ul>
      </div>
    </div>
  )
}

export default Home
