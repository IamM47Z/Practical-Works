all:
	cd frontend && $(MAKE)
	cd backend && $(MAKE)

clean:
	cd frontend && $(MAKE) clean
	cd backend && $(MAKE) clean
ifneq ($(wildcard objects/.*),)
	rm -r objects/
endif
