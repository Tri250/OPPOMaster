import { motion } from 'framer-motion';
import { Send, Bot, User, Loader, Settings, Trash2, Copy, CheckCheck } from 'lucide-react';
import { useState, useRef, useEffect, useCallback } from 'react';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

const SYSTEM_PROMPT = `你是一个专业的影像参数助手，专注于OPPO/一加手机的哈苏影像系统调色参数优化。你的名字叫"小O"，你可以帮助用户：

1. 推荐最佳的影像预设参数
2. 解释各种相机参数的含义
3. 根据场景（人像、风景、夜景等）给出调色建议
4. 帮助用户理解和应用HNCS色彩解决方案

请用专业且友好的语气回答问题。`;

const API_URL = 'https://api.deepseek.com/chat/completions';
const DEFAULT_MODEL = 'deepseek-chat';

export default function DeepSeekChatPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      role: 'assistant',
      content: `你好！我是小O，基于DeepSeek大模型驱动的影像参数助手。我可以帮助你：

• 推荐最佳影像预设参数
• 解答相机参数问题
• 提供场景调色建议
• 解读HNCS色彩方案

有什么关于影像参数的问题吗？`,
      timestamp: new Date()
    }
  ]);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [apiKey, setApiKey] = useState(() => localStorage.getItem('deepseek_api_key') || '');
  const [showSettings, setShowSettings] = useState(false);
  const [model, setModel] = useState(DEFAULT_MODEL);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  const handleSaveApiKey = (key: string) => {
    setApiKey(key);
    localStorage.setItem('deepseek_api_key', key);
    setShowSettings(false);
  };

  const handleCopyMessage = async (messageId: string, content: string) => {
    try {
      await navigator.clipboard.writeText(content);
      setCopiedId(messageId);
      setTimeout(() => setCopiedId(null), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  const handleClearChat = () => {
    setMessages([
      {
        id: Date.now().toString(),
        role: 'assistant',
        content: SYSTEM_PROMPT.split('\n').slice(2).join('\n').trim(),
        timestamp: new Date()
      }
    ]);
  };

  const handleSendMessage = async () => {
    if (!inputValue.trim() || isLoading) return;

    if (!apiKey) {
      setShowSettings(true);
      return;
    }

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: inputValue.trim(),
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setInputValue('');
    setIsLoading(true);

    try {
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${apiKey}`
        },
        body: JSON.stringify({
          model: model,
          messages: [
            { role: 'system', content: SYSTEM_PROMPT },
            ...messages.map(m => ({ role: m.role, content: m.content })),
            { role: 'user', content: userMessage.content }
          ],
          stream: false,
          temperature: 0.7,
          max_tokens: 2048
        })
      });

      if (!response.ok) {
        throw new Error(`API Error: ${response.status}`);
      }

      const data = await response.json();
      const assistantContent = data.choices?.[0]?.message?.content || '抱歉，我暂时无法回答这个问题。';

      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: assistantContent,
        timestamp: new Date()
      };

      setMessages(prev => [...prev, assistantMessage]);
    } catch (error) {
      console.error('DeepSeek API Error:', error);
      const errorMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: `请求失败: ${error instanceof Error ? error.message : '未知错误'}。请检查API密钥是否正确，或稍后重试。`,
        timestamp: new Date()
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto h-[calc(100vh-10rem)] flex flex-col">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between mb-6"
        >
          <div className="flex items-center space-x-4">
            <div className="w-14 h-14 bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl flex items-center justify-center">
              <Bot className="w-8 h-8 text-white" />
            </div>
            <div>
              <h1 className="text-3xl font-bold gradient-text">DeepSeek AI助手</h1>
              <p className="text-white/60 text-sm">基于DeepSeek大模型的智能影像参数助手</p>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={handleClearChat}
              className="p-3 rounded-xl bg-white/5 hover:bg-white/10 transition-colors"
              title="清空对话"
            >
              <Trash2 className="w-5 h-5 text-white/60" />
            </motion.button>
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => setShowSettings(!showSettings)}
              className="p-3 rounded-xl bg-white/5 hover:bg-white/10 transition-colors"
              title="API设置"
            >
              <Settings className={`w-5 h-5 text-white/60 ${showSettings ? 'text-hasselblad' : ''}`} />
            </motion.button>
          </div>
        </motion.div>

        {/* Settings Panel */}
        {showSettings && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            className="card p-6 mb-6 space-y-4"
          >
            <h3 className="font-semibold text-lg">API配置</h3>
            <div className="space-y-3">
              <div>
                <label className="block text-sm text-white/60 mb-2">DeepSeek API Key</label>
                <input
                  type="password"
                  placeholder="请输入您的DeepSeek API Key"
                  defaultValue={apiKey}
                  onBlur={(e) => handleSaveApiKey(e.target.value)}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-white/40 outline-none focus:border-hasselblad transition-colors"
                />
              </div>
              <div>
                <label className="block text-sm text-white/60 mb-2">模型</label>
                <select
                  value={model}
                  onChange={(e) => setModel(e.target.value)}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white outline-none focus:border-hasselblad transition-colors"
                >
                  <option value="deepseek-chat">deepseek-chat</option>
                  <option value="deepseek-coder">deepseek-coder</option>
                </select>
              </div>
              <p className="text-xs text-white/40">
                API Key将保存在浏览器本地，不会上传到任何服务器。
              </p>
            </div>
          </motion.div>
        )}

        {/* Messages Container */}
        <div className="flex-1 overflow-y-auto space-y-4 mb-4 pr-2" style={{ scrollbarWidth: 'thin' }}>
          {messages.map((message, index) => (
            <motion.div
              key={message.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
              className={`flex items-start space-x-3 ${message.role === 'user' ? 'flex-row-reverse' : ''}`}
            >
              {/* Avatar */}
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${
                message.role === 'user' 
                  ? 'bg-gradient-to-br from-oppo-green to-cyan-500' 
                  : 'bg-gradient-to-br from-blue-500 to-purple-600'
              }`}>
                {message.role === 'user' ? (
                  <User className="w-5 h-5 text-white" />
                ) : (
                  <Bot className="w-5 h-5 text-white" />
                )}
              </div>

              {/* Message Content */}
              <div className={`flex-1 max-w-[80%] ${message.role === 'user' ? 'text-right' : ''}`}>
                <div className={`inline-block p-4 rounded-2xl ${
                  message.role === 'user'
                    ? 'bg-gradient-to-br from-oppo-green to-cyan-500 text-white'
                    : 'bg-white/5 border border-white/10'
                }`}>
                  <p className="text-sm leading-relaxed whitespace-pre-wrap">{message.content}</p>
                </div>
                
                {/* Message Actions */}
                {message.role === 'assistant' && (
                  <div className="flex items-center space-x-2 mt-2">
                    <motion.button
                      whileHover={{ scale: 1.05 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => handleCopyMessage(message.id, message.content)}
                      className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 transition-colors"
                      title="复制"
                    >
                      {copiedId === message.id ? (
                        <CheckCheck className="w-4 h-4 text-oppo-green" />
                      ) : (
                        <Copy className="w-4 h-4 text-white/40" />
                      )}
                    </motion.button>
                    <span className="text-xs text-white/30">
                      {message.timestamp.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                )}
              </div>
            </motion.div>
          ))}

          {/* Loading Indicator */}
          {isLoading && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="flex items-start space-x-3"
            >
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
                <Bot className="w-5 h-5 text-white" />
              </div>
              <div className="bg-white/5 border border-white/10 p-4 rounded-2xl">
                <div className="flex items-center space-x-2">
                  <Loader className="w-4 h-4 text-hasselblad animate-spin" />
                  <span className="text-sm text-white/60">思考中...</span>
                </div>
              </div>
            </motion.div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input Area */}
        <div className="card p-4">
          <div className="flex items-end space-x-3">
            <div className="flex-1 relative">
              <textarea
                ref={textareaRef}
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder={apiKey ? "输入消息..." : "请先设置DeepSeek API Key"}
                disabled={!apiKey}
                rows={1}
                className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 pr-12 text-white placeholder-white/40 outline-none focus:border-hasselblad transition-colors resize-none disabled:opacity-50"
                style={{ minHeight: '48px', maxHeight: '120px' }}
              />
            </div>
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={handleSendMessage}
              disabled={!inputValue.trim() || isLoading || !apiKey}
              className={`p-3 rounded-xl transition-all ${
                inputValue.trim() && !isLoading && apiKey
                  ? 'bg-gradient-to-br from-hasselblad to-orange-500 hover:shadow-lg hover:shadow-hasselblad/20'
                  : 'bg-white/10 cursor-not-allowed'
              }`}
            >
              <Send className={`w-5 h-5 ${inputValue.trim() && apiKey ? 'text-white' : 'text-white/40'}`} />
            </motion.button>
          </div>
          <p className="text-xs text-white/30 mt-2 text-center">
            按Enter发送，Shift+Enter换行
          </p>
        </div>
      </div>
    </div>
  );
}
