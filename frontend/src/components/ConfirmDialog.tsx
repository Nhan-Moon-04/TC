import { useTranslation } from 'react-i18next'
import Modal from './Modal'

interface ConfirmDialogProps {
  isOpen: boolean
  onClose: () => void
  onConfirm: () => void
  title?: string
  message?: string
  danger?: boolean
}

export default function ConfirmDialog({ isOpen, onClose, onConfirm, title, message, danger = false }: ConfirmDialogProps) {
  const { t } = useTranslation()

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title || t('common.confirm')} size="sm">
      <p className="text-gray-600 text-sm mb-6">{message || t('common.confirmDelete')}</p>
      <div className="flex gap-3 justify-end">
        <button onClick={onClose} className="btn btn-secondary">{t('common.cancel')}</button>
        <button
          onClick={() => { onConfirm(); onClose() }}
          className={danger ? 'btn btn-danger' : 'btn btn-primary'}
        >
          {t('common.confirm')}
        </button>
      </div>
    </Modal>
  )
}
