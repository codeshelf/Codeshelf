/**
 * Transforms the values of a PARSED line in an order file.
 * The fields available on the OrderLine object match the documented fields for
 * an order import file
 */
def OrderImportBeanTransformation(orderLine) {
    shipperIdNoScan = ['UPS', 'ANOTHERSHIPPER']
    if (shipperIdNoScan.contains(orderLine.shipperId)) {
        orderLine.needsScan = false;
    } else {
        orderLine.needsScan = true;
   }
    return orderLine;
}
