#include <stdint.h>
#include <gpio.h>
#include <soc.h>

#define GPIO_REG_WRITEENABLE_ADDR   GPIO0_BASE + GPIO_REG_WRITEENABLE
#define GPIO_REG_WRITE_ADDR         GPIO0_BASE + GPIO_REG_WRITE

int main(int argc, char **argv)
{
    int                 i;
    uint32_t*           gpio_ptr;
    volatile uint32_t   value;

    // setup the enable register
    gpio_ptr = GPIO_REG_WRITEENABLE_ADDR;
    *gpio_ptr = 15;

    // write to enable led
    value = 0;
    gpio_ptr = GPIO_REG_WRITE_ADDR;
    while(1) {
        *gpio_ptr = value;
        value = value + 1;
        for (i = 0; i < 10000000; i++);
    }
    return 0;
}