import { useState } from 'react'
import { Send, Loader2 } from 'lucide-react'

const methods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']

export default function ApiTest() {
  const [method, setMethod] = useState('GET')
  const [url, setUrl] = useState('/api/status')
  const [headers, setHeaders] = useState('{}')
  const [body, setBody] = useState('{}')
  const [response, setResponse] = useState<any>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const sendRequest = async () => {
    setLoading(true)
    setError('')
    setResponse(null)

    try {
      const options: RequestInit = {
        method,
        headers: {
          'Content-Type': 'application/json',
          ...JSON.parse(headers)
        }
      }

      if (method !== 'GET' && method !== 'DELETE') {
        options.body = body
      }

      const startTime = performance.now()
      const res = await fetch(url, options)
      const endTime = performance.now()

      const data = await res.json()
      setResponse({
        status: res.status,
        statusText: res.statusText,
        time: Math.round(endTime - startTime),
        data
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : '请求失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">API测试工具</h1>
        <p className="text-gray-400 mt-1">测试后端API接口</p>
      </div>

      {/* Request Builder */}
      <div className="bg-[#161B22] rounded-xl border border-[#30363D] p-6 space-y-4">
        <div className="flex gap-3">
          <select
            value={method}
            onChange={(e) => setMethod(e.target.value)}
            className="bg-[#0D1117] border border-[#30363D] rounded-lg px-3 py-2 text-white font-mono"
          >
            {methods.map(m => (
              <option key={m} value={m}>{m}</option>
            ))}
          </select>
          <input
            type="text"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="输入API路径"
            className="flex-1 bg-[#0D1117] border border-[#30363D] rounded-lg px-3 py-2 text-white font-mono focus:outline-none focus:border-[#FF6B35]"
          />
          <button
            onClick={sendRequest}
            disabled={loading}
            className="flex items-center gap-2 px-6 py-2 bg-[#FF6B35] hover:bg-[#FF8C42] disabled:opacity-50 text-white rounded-lg font-medium transition-colors"
          >
            {loading ? <Loader2 size={18} className="animate-spin" /> : <Send size={18} />}
            发送
          </button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-2">Headers (JSON)</label>
            <textarea
              value={headers}
              onChange={(e) => setHeaders(e.target.value)}
              rows={6}
              className="w-full bg-[#0D1117] border border-[#30363D] rounded-lg px-3 py-2 text-white font-mono text-sm focus:outline-none focus:border-[#FF6B35]"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-2">Body (JSON)</label>
            <textarea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              rows={6}
              className="w-full bg-[#0D1117] border border-[#30363D] rounded-lg px-3 py-2 text-white font-mono text-sm focus:outline-none focus:border-[#FF6B35]"
            />
          </div>
        </div>
      </div>

      {/* Response */}
      {(response || error) && (
        <div className="bg-[#161B22] rounded-xl border border-[#30363D] p-6">
          <h2 className="text-lg font-semibold text-white mb-4">响应结果</h2>
          
          {error ? (
            <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400">
              {error}
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex gap-4 text-sm">
                <span className={`px-2 py-1 rounded ${response?.status < 400 ? 'bg-green-500/10 text-green-400' : 'bg-red-500/10 text-red-400'}`}>
                  Status: {response?.status} {response?.statusText}
                </span>
                <span className="px-2 py-1 rounded bg-blue-500/10 text-blue-400">
                  Time: {response?.time}ms
                </span>
              </div>
              <pre className="bg-[#0D1117] rounded-lg p-4 overflow-x-auto text-sm text-gray-300 font-mono">
                {JSON.stringify(response?.data, null, 2)}
              </pre>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
