window.SettingsDeleteAccount = (function () {
    // Wire account deletion modal.
    function initDeleteAccount() {
        const deleteBtn = document.getElementById('settings-delete-account-button');
        const deleteOverlay = document.getElementById('delete-account-overlay');
        const deleteModal = document.getElementById('delete-account-modal');
        const deleteCancel = document.getElementById('delete-account-cancel');
        const deleteConfirm = document.getElementById('delete-account-confirm');

        if (!deleteBtn || !deleteOverlay || !deleteModal || !deleteCancel || !deleteConfirm) {
            return;
        }

        const closeDeleteModal = () => {
            deleteOverlay.classList.add('hidden');
            deleteModal.classList.add('hidden');
        };

        deleteBtn.addEventListener('click', () => {
            deleteOverlay.classList.remove('hidden');
            deleteModal.classList.remove('hidden');
        });

        deleteCancel.addEventListener('click', closeDeleteModal);
        deleteOverlay.addEventListener('click', closeDeleteModal);

        deleteConfirm.addEventListener('click', async () => {
            deleteConfirm.disabled = true;
            deleteConfirm.textContent = 'Deleting...';

            try {
                const response = await fetch('/api/auth/delete-account', {
                    method: 'POST',
                    headers: {
                        Accept: 'application/json',
                        ...(window.getCsrfHeaders ? window.getCsrfHeaders() : {})
                    }
                });

                if (response.ok) {
                    window.location.href = '/login?deleted=true';
                } else {
                    throw new Error('Deletion failed');
                }
            } catch (error) {
                console.error(error);
                deleteConfirm.disabled = false;
                deleteConfirm.textContent = 'Confirm Deletion';
                alert('Could not process your request. Please try again later.');
            }
        });
    }

    return {
        initDeleteAccount
    };
})();
