/**
 * This is a sample Bean extension point
 * Transforms the values of a PARSED line in an order file.
 * The fields available on the OrderLine object match the documented fields for
 * an order import file
 */
def OrderImportBeanTransformation(orderLine) {
    customerNeedsScan = ['SPECIALCUSTOMER', 'SPECIALCUSTOMER2']
    if (customerNeedsScan.contains(orderLine.customerId)) {
        orderLine.needsScan = true;
    }
    return orderLine;
}
