// app/src/main/java/in/merakisan/app/ui/marketplace/CheckoutBottomSheetFragment.kt (मुख्य सुरक्षा विधि)
private fun setupPaymentOptions() {
    val isPaymentGatewayEnabled = FeatureManager.isEnabled(requireContext(), "payment")
    if (!isPaymentGatewayEnabled) {
        binding.rbPaymentOnline.visibility = View.GONE
        binding.rbPaymentCod.isChecked = true
        binding.tvPaymentNote.text = "वर्तमान में केवल डिलीवरी पर भुगतान (COD) या किसान से सीधा संपर्क मान्य है।"
    } else {
        binding.rbPaymentOnline.visibility = View.VISIBLE
    }
}
